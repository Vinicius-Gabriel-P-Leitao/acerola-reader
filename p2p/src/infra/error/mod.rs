#[cfg(feature = "iroh")]
pub(crate) mod iroh;
#[cfg(feature = "iroh-blobs-adapter")]
pub(crate) mod iroh_blobs;
pub(crate) mod rpc;

use thiserror::Error;

use crate::infra::peer::PeerId;

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
    #[error("connection denied by guard: {0}")]
    AuthDenied(String),

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
    #[error("peer disconnected: {0}")]
    PeerDisconnected(String),

    /// A conexão falhou pois as partes não suportam a mesma versão do protocolo base.
    #[error("incompatible protocol version")]
    IncompatibleVersion,

    /// Falha na alocação de recursos locais ao iniciar a rede (ex: portas em uso).
    #[error("failed to initialize connection: {0}")]
    StartupFailed(String),

    /// O hash de blob solicitado não está disponível no store local.
    #[error("blob not found: {0}")]
    BlobNotFound(String),
}

/// Erros restritos ao contexto de RPC (Remote Procedure Call) e framings customizados.
#[derive(Debug, Error)]
pub enum RpcError {
    /// Houve uma falha ou fechamento prematuro durante o processamento do protocolo.
    #[error("stream error: {0}")]
    Stream(String),
}

/// Erros na camda de entrevista do dispositivo.
#[derive(Debug, Error)]
pub enum DeviceInfoError {
    /// Ao buscar o nome do dispositivo obtemos erro.
    #[error("failed to read device name")]
    Name,

    /// Busca de qual SO a applicação está rodando falhou.
    #[error("failed to read operating system")]
    Os,

    /// Falaha ao pegar versão do app a qual o dispositivo roda.
    #[error("failed to read app version")]
    Version,
}

impl From<getrandom::Error> for ConnectionError {
    fn from(err: getrandom::Error) -> Self {
        tracing::error!(layer = "infra", error = ?err, "system failed to provide secure entropy");
        ConnectionError::StartupFailed("system cannot provide secure entropy".into())
    }
}

#[cfg(test)]
mod tests {
    use super::{ConnectionError, DeviceInfoError, RpcError};
    use crate::infra::peer::PeerId;

    /// Verifica que RpcError::Stream exibe "stream error" e a mensagem interna.
    #[test]
    fn rpc_error_stream_display() {
        let error_message = RpcError::Stream("framing failure".into()).to_string();

        assert!(error_message.contains("stream error"));
        assert!(error_message.contains("framing failure"));
    }

    /// Verifica que PeerNotFound exibe "peer not found" e o id do peer.
    #[test]
    fn connection_error_peer_not_found_display() {
        let peer_id = PeerId { id: "abc123".into(), device_id: None };
        let error_message = ConnectionError::PeerNotFound(peer_id).to_string();

        assert!(error_message.contains("peer not found"));
        assert!(error_message.contains("abc123"));
    }

    /// Verifica que AuthDenied exibe "connection denied by guard" e o motivo.
    #[test]
    fn connection_error_auth_denied_display() {
        let error_message = ConnectionError::AuthDenied("blocklist".into()).to_string();

        assert!(error_message.contains("connection denied by guard"));
        assert!(error_message.contains("blocklist"));
    }

    /// Verifica que StreamFailed exibe "stream failed" e a descrição do erro.
    #[test]
    fn connection_error_stream_failed_display() {
        let error_message = ConnectionError::StreamFailed("io error".into()).to_string();

        assert!(error_message.contains("stream failed"));
        assert!(error_message.contains("io error"));
    }

    /// Verifica que Shutdown exibe exatamente "endpoint shut down".
    #[test]
    fn connection_error_shutdown_display() {
        assert_eq!(ConnectionError::Shutdown.to_string(), "endpoint shut down");
    }

    /// Verifica que Timeout exibe exatamente "connection timed out".
    #[test]
    fn connection_error_timeout_display() {
        assert_eq!(ConnectionError::Timeout.to_string(), "connection timed out");
    }

    /// Verifica que PeerDisconnected exibe "peer disconnected" e a mensagem passada.
    #[test]
    fn connection_error_peer_disconnected_display() {
        let disconnect_reason = "connection reset by peer".to_string();
        let error_message =
            ConnectionError::PeerDisconnected(disconnect_reason.clone()).to_string();

        assert!(error_message.contains("peer disconnected"));
        assert!(error_message.contains(&disconnect_reason));
    }

    /// Verifica que IncompatibleVersion exibe exatamente "incompatible protocol version".
    #[test]
    fn connection_error_incompatible_version_display() {
        assert_eq!(
            ConnectionError::IncompatibleVersion.to_string(),
            "incompatible protocol version"
        );
    }

    /// Verifica que StartupFailed exibe "failed to initialize connection" e a mensagem passada.
    #[test]
    fn connection_error_startup_failed_display() {
        let startup_message = "port already in use".to_string();
        let error_message = ConnectionError::StartupFailed(startup_message.clone()).to_string();

        assert!(error_message.contains("failed to initialize connection"));
        assert!(error_message.contains(&startup_message));
    }

    /// Verifica que DeviceInfoError::Name exibe "failed to read device name".
    #[test]
    fn device_info_error_name_display() {
        assert_eq!(DeviceInfoError::Name.to_string(), "failed to read device name");
    }

    /// Verifica que DeviceInfoError::Os exibe "failed to read operating system".
    #[test]
    fn device_info_error_os_display() {
        assert_eq!(DeviceInfoError::Os.to_string(), "failed to read operating system");
    }

    /// Verifica que DeviceInfoError::Version exibe "failed to read app version".
    #[test]
    fn device_info_error_version_display() {
        assert_eq!(DeviceInfoError::Version.to_string(), "failed to read app version");
    }

    /// Verifica que a conversão de getrandom::Error produz ConnectionError::StartupFailed.
    #[test]
    fn from_getrandom_error_produces_startup_failed() {
        let converted_error = ConnectionError::from(getrandom::Error::UNSUPPORTED);

        assert!(matches!(converted_error, ConnectionError::StartupFailed(_)));
    }

    /// Verifica que BlobNotFound exibe "blob not found" e o hash informado.
    #[test]
    fn connection_error_blob_not_found_display() {
        let error_message = ConnectionError::BlobNotFound("abc123".to_string()).to_string();

        assert!(error_message.contains("blob not found"));
        assert!(error_message.contains("abc123"));
    }
}
