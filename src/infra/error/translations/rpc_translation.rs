use crate::infra::error::messages::rpc_error::RpcError;

impl From<std::io::Error> for RpcError {
    fn from(err: std::io::Error) -> Self {
        RpcError::Stream(err.to_string())
    }
}
