use super::{ConnectionError, RpcError};

impl From<std::io::Error> for RpcError {
    fn from(err: std::io::Error) -> Self {
        RpcError::Stream(err.to_string())
    }
}

impl From<serde_json::Error> for RpcError {
    fn from(err: serde_json::Error) -> Self {
        RpcError::Stream(format!("serialization error: {err}"))
    }
}

impl From<RpcError> for ConnectionError {
    fn from(err: RpcError) -> Self {
        match err {
            RpcError::Stream(msg) => ConnectionError::StreamFailed(msg),
        }
    }
}
