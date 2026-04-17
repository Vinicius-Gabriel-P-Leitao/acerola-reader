use crate::infra::remote::types::PeerId;
use iroh::endpoint::BindError as IrohBindError;
use iroh::endpoint::ConnectError as IrohConnectError;
use iroh::endpoint::ConnectionError as IrohConnectionError;

use thiserror::Error;

// TODO: Documentar e converter para ingles
#[derive(Debug, Error)]
pub enum ConnectionError {
    #[error("peer não encontrado: {0}")]
    PeerNotFound(PeerId),

    #[error("conexão recusada pelo guard")]
    AuthDenied,

    #[error("falha no stream: {0}")]
    StreamFailed(String),

    #[error("endpoint encerrado")]
    Shutdown,

    #[error("tempo de conexão esgotado")]
    Timeout,

    #[error("peer desconectado")]
    PeerDisconnected,

    #[error("versão de protocolo incompatível")]
    IncompatibleVersion,

    #[error("erro ao inicializar a conex: {0}")]
    StartupFailed(String),
}

impl From<IrohConnectionError> for ConnectionError {
    fn from(connection_err: IrohConnectionError) -> Self {
        match connection_err {
            IrohConnectionError::TimedOut => ConnectionError::Timeout,
            IrohConnectionError::Reset => ConnectionError::PeerDisconnected,
            IrohConnectionError::ConnectionClosed(_) => ConnectionError::PeerDisconnected,
            IrohConnectionError::ApplicationClosed(_) => ConnectionError::PeerDisconnected,
            IrohConnectionError::VersionMismatch => ConnectionError::IncompatibleVersion,
            IrohConnectionError::LocallyClosed => ConnectionError::Shutdown,
            connection_err => ConnectionError::StreamFailed(connection_err.to_string()),
        }
    }
}

impl From<IrohConnectError> for ConnectionError {
    fn from(connect_err: IrohConnectError) -> Self {
        match connect_err {
            IrohConnectError::Connection { source, .. } => ConnectionError::from(source),
            IrohConnectError::Connect { meta, .. } => {
                log::debug!(
                    "[IrohTransport::ConnectError] falha ao iniciar conexão — meta: {:?}",
                    meta
                );
                ConnectionError::PeerDisconnected
            }
            IrohConnectError::Connecting { meta, .. } => {
                log::debug!(
                    "[IrohTransport::ConnectError] falha no handshake — meta: {:?}",
                    meta
                );
                ConnectionError::PeerDisconnected
            }
            connect_err => {
                log::debug!(
                    "[IrohTransport::ConnectError] erro não mapeado: {:?}",
                    connect_err
                );
                ConnectionError::StreamFailed(connect_err.to_string())
            }
        }
    }
}

impl From<IrohBindError> for ConnectionError {
    fn from(bind_err: IrohBindError) -> Self {
        match bind_err {
            IrohBindError::Sockets { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] falha ao fazer bind nos sockets — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("porta indisponível".into())
            }
            IrohBindError::CreateQuicEndpoint { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] falha ao criar endpoint QUIC — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("falha ao criar endpoint QUIC".into())
            }
            IrohBindError::CreateNetmonMonitor { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] falha ao criar monitor de rede — meta: {:?}",
                    meta
                );
                ConnectionError::StreamFailed("monitor de rede indisponível".into())
            }
            IrohBindError::InvalidTransportConfig { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] configuração de transporte inválida — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("configuração de transporte inválida".into())
            }
            IrohBindError::InvalidCaRootConfig { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] configuração de certificado inválida — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("configuração de certificado inválida".into())
            }
            bind_err => {
                log::debug!(
                    "[IrohTransport::BindError] erro não mapeado: {:?}",
                    bind_err
                );
                ConnectionError::StreamFailed(bind_err.to_string())
            }
        }
    }
}
