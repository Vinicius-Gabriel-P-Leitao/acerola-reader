use super::RpcError;

impl From<serde_json::Error> for RpcError {
    fn from(serde_err: serde_json::Error) -> Self {
        match serde_err.classify() {
            serde_json::error::Category::Io => RpcError::Stream(serde_err.to_string()),
            serde_json::error::Category::Syntax
            | serde_json::error::Category::Data
            | serde_json::error::Category::Eof => RpcError::Deserialize(serde_err.to_string()),
        }
    }
}

impl From<std::io::Error> for RpcError {
    fn from(err: std::io::Error) -> Self {
        RpcError::Stream(err.to_string())
    }
}
