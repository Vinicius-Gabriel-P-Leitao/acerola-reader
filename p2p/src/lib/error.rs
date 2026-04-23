//! Definições de erro unificadas da biblioteca.
//!
//! Concentra o mapeamento dos erros específicos de transportes (como Iroh)
//! e os traduz para uma representação agnóstica (`ConnectionError`). Isso
//! garante que a aplicação não dependa de bibliotecas de transporte diretamente.

use iroh::{
    endpoint::{
        BindError as IrohBindError, ConnectError as IrohConnectError,
        ConnectingError as IrohConnectingError, ConnectionError as IrohConnectionError,
    },
    RelayUrlParseError,
};
use thiserror::Error;

use crate::peer::PeerId;

/// Erros relacionados ao ciclo de vida e estabelecimento de conexões P2P.
///
/// O `ConnectionError` unifica falhas de roteamento, rejeição de autenticação,
/// erros de socket e desconexões abruptas numa enumeração clara.
#[derive(Debug, Error)]
pub enum ConnectionError {
    /// O nó especificado não pôde ser encontrado na rede (falha na resolução de endereço).
    #[error("peer not found: {0}")]
    PeerNotFound(PeerId),

    /// A conexão foi rejeitada pelo Guard customizado da aplicação (falha de autorização).
    #[error("connection denied by guard")]
    AuthDenied,

    /// Ocorreu um erro interno de I/O em uma stream ativa.
    #[error("stream failed: {0}")]
    StreamFailed(String),

    /// O serviço de rede foi desligado intencionalmente.
    #[error("endpoint shut down")]
    Shutdown,

    /// Excedeu-se o tempo limite estipulado para a conexão.
    #[error("connection timed out")]
    Timeout,

    /// O par remoto encerrou ou resetou a conexão repentinamente.
    #[error("peer disconnected")]
    PeerDisconnected,

    /// A conexão falhou pois as partes não suportam a mesma versão do protocolo base.
    #[error("incompatible protocol version")]
    IncompatibleVersion,

    /// Falha na alocação de recursos locais ao iniciar a rede (ex: portas em uso).
    #[error("failed to initialize connection: {0}")]
    StartupFailed(String),
}

/// Erros restritos ao contexto de RPC (Remote Procedure Call) e framings customizados.
#[derive(Debug, Error)]
pub enum RpcError {
    /// Houve uma falha ou fechamento prematuro durante o processamento do protocolo.
    #[error("stream error: {0}")]
    Stream(String),
}

impl From<std::io::Error> for RpcError {
    fn from(err: std::io::Error) -> Self {
        RpcError::Stream(err.to_string())
    }
}

impl From<RpcError> for ConnectionError {
    fn from(err: RpcError) -> Self {
        match err {
            RpcError::Stream(msg) => ConnectionError::StreamFailed(msg),
        }
    }
}

// -----------------------------------------------------------------------------
// Conversões de erros do Iroh
// Transformam falhas detalhadas do protocolo QUIC/Iroh em um formato legível.
// -----------------------------------------------------------------------------

impl From<IrohBindError> for ConnectionError {
    fn from(err: IrohBindError) -> Self {
        match err {
            IrohBindError::Sockets { meta, .. } => {
                log::debug!("[IrohTransport::BindError] failed to bind sockets — meta: {:?}", meta);
                ConnectionError::StartupFailed("port unavailable".into())
            },
            IrohBindError::CreateQuicEndpoint { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] failed to create QUIC endpoint — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("failed to create QUIC endpoint".into())
            },
            IrohBindError::CreateNetmonMonitor { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] failed to create network monitor — meta: {:?}",
                    meta
                );
                ConnectionError::StreamFailed("network monitor unavailable".into())
            },
            IrohBindError::InvalidTransportConfig { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] invalid transport configuration — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("invalid transport configuration".into())
            },
            IrohBindError::InvalidCaRootConfig { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] invalid CA root configuration — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("invalid certificate configuration".into())
            },
            err => {
                log::debug!("[IrohTransport::BindError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<IrohConnectError> for ConnectionError {
    fn from(err: IrohConnectError) -> Self {
        match err {
            IrohConnectError::Connection { source, .. } => ConnectionError::from(source),
            IrohConnectError::Connect { meta, .. } => {
                log::debug!(
                    "[IrohTransport::ConnectError] failed to initiate connection — meta: {:?}",
                    meta
                );
                ConnectionError::PeerDisconnected
            },
            IrohConnectError::Connecting { meta, .. } => {
                log::debug!("[IrohTransport::ConnectError] handshake failed — meta: {:?}", meta);
                ConnectionError::PeerDisconnected
            },
            err => {
                log::debug!("[IrohTransport::ConnectError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<IrohConnectingError> for ConnectionError {
    fn from(err: IrohConnectingError) -> Self {
        match err {
            IrohConnectingError::ConnectionError { source, .. } => ConnectionError::from(source),
            IrohConnectingError::LocallyRejected { .. } => {
                log::warn!("[IrohTransport::ConnectingError] connection rejected locally by guard");
                ConnectionError::AuthDenied
            },
            IrohConnectingError::HandshakeFailure { .. } => {
                log::warn!(
                    "[IrohTransport::ConnectingError] handshake failed — invalid or untrusted peer"
                );
                ConnectionError::AuthDenied
            },
            IrohConnectingError::InternalConsistencyError { .. } => {
                log::debug!("[IrohTransport::ConnectingError] internal consistency error");
                ConnectionError::StreamFailed("internal error".into())
            },
            err => {
                log::debug!("[IrohTransport::ConnectingError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<IrohConnectionError> for ConnectionError {
    fn from(err: IrohConnectionError) -> Self {
        match err {
            IrohConnectionError::TimedOut => {
                log::warn!("[IrohTransport::ConnectionError] connection timed out");
                ConnectionError::Timeout
            },
            IrohConnectionError::Reset => {
                log::warn!("[IrohTransport::ConnectionError] connection reset by peer");
                ConnectionError::PeerDisconnected
            },
            IrohConnectionError::ConnectionClosed(_) => {
                log::debug!("[IrohTransport::ConnectionError] connection closed by peer");
                ConnectionError::PeerDisconnected
            },
            IrohConnectionError::ApplicationClosed(_) => {
                log::debug!("[IrohTransport::ConnectionError] connection closed by application");
                ConnectionError::PeerDisconnected
            },
            IrohConnectionError::VersionMismatch => {
                log::warn!("[IrohTransport::ConnectionError] incompatible protocol version");
                ConnectionError::IncompatibleVersion
            },
            IrohConnectionError::LocallyClosed => {
                log::debug!("[IrohTransport::ConnectionError] connection closed locally");
                ConnectionError::Shutdown
            },
            err => {
                log::debug!("[IrohTransport::ConnectionError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<RelayUrlParseError> for ConnectionError {
    fn from(relay_err: RelayUrlParseError) -> Self {
        log::debug!(
            "[IrohTransport::RelayUrlParseError] failed to parse relay URL — error: {:?}",
            relay_err
        );

        ConnectionError::StartupFailed("invalid relay URL".into())
    }
}
