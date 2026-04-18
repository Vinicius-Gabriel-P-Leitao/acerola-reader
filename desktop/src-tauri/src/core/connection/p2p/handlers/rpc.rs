use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};

use crate::infra::{
    error::messages::{connection_error::ConnectionError, rpc_error::RpcError},
    remote::p2p::{peer_id::PeerId, protocol_handler::ProtocolHandler},
};

#[derive(Debug, Serialize, Deserialize)]
pub enum RpcRequest {
    Ping,
}

#[derive(Debug, Serialize, Deserialize)]
pub enum RpcResponse {
    Pong,
}

pub struct RpcHandler;

impl RpcHandler {
    pub fn new() -> Self {
        Self
    }

    async fn read_request(
        recv: &mut (impl AsyncRead + Unpin),
    ) -> Result<RpcRequest, RpcError> {
        let mut len_buf = [0u8; 4];
        recv.read_exact(&mut len_buf).await?;

        let len = u32::from_be_bytes(len_buf) as usize;
        let mut buf = vec![0u8; len];
        recv.read_exact(&mut buf).await?;

        Ok(serde_json::from_slice(&buf)?)
    }

    async fn write_response(
        send: &mut (impl AsyncWrite + Unpin), response: &RpcResponse,
    ) -> Result<(), RpcError> {
        let bytes = serde_json::to_vec(response)?;

        let len = bytes.len() as u32;
        send.write_all(&len.to_be_bytes()).await?;
        send.write_all(&bytes).await?;

        Ok(())
    }
}

#[async_trait]
impl ProtocolHandler for RpcHandler {
    async fn handle(
        &self, peer: &PeerId, mut send: Box<dyn AsyncWrite + Send + Unpin>,
        mut recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError> {
        loop {
            match Self::read_request(&mut recv).await {
                Ok(RpcRequest::Ping) => {
                    log::debug!("[RpcHandler] ping from {}", peer.id);
                    Self::write_response(&mut send, &RpcResponse::Pong).await?;
                },
                Err(_) => break,
            }
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_ping_serializa_e_deserializa() {
        let request = RpcRequest::Ping;
        let bytes = serde_json::to_vec(&request).unwrap();
        let decoded: RpcRequest = serde_json::from_slice(&bytes).unwrap();

        assert!(matches!(decoded, RpcRequest::Ping));
    }

    #[tokio::test]
    async fn test_pong_serializa_e_deserializa() {
        let response = RpcResponse::Pong;
        let bytes = serde_json::to_vec(&response).unwrap();
        let decoded: RpcResponse = serde_json::from_slice(&bytes).unwrap();

        assert!(matches!(decoded, RpcResponse::Pong));
    }
}
