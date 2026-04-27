pub(crate) mod iroh;
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

/// Erros na camda de entrevista do dispositivo.
#[derive(Debug, Error)]
pub enum DeviceInfoError {
    /// Ao buscar o nome do dispositivo obtemos erro.
    #[error("failed to read device name")]
    NameUnavailable,

    /// Busca de qual SO a applicação está rodando falhou.
    #[error("failed to read operating system")]
    OsUnavailable,

    /// Falaha ao pegar versão do app a qual o dispositivo roda.
    #[error("failed to read app version")]
    VersionUnavailable,
}
