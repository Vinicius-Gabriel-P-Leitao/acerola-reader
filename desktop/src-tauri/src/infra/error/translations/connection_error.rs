use iroh::endpoint::BindError;
use thiserror::Error;

use crate::infra::remote::types::PeerId;

// TODO: Documentar
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
}

impl From<BindError> for ConnectionError {
    fn from(bind_err: BindError) -> Self {
        log::error!("[IrohTransport] falha ao iniciar endpoint: {}", bind_err);
        ConnectionError::StreamFailed(bind_err.to_string())
    }
}
