use thiserror::Error;

#[derive(Debug, Error)]
pub enum RpcError {
    #[error("failed to serialize message: {0}")]
    Serialize(String),

    #[error("failed to deserialize message: {0}")]
    Deserialize(String),

    #[error("stream error: {0}")]
    Stream(String),
}
