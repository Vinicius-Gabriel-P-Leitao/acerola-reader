use thiserror::Error;

#[derive(Debug, Error)]
pub enum RpcError {
    #[error("stream error: {0}")]
    Stream(String),
}
