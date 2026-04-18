use async_trait::async_trait;
use futures::sink::SinkExt;
use serde::{Deserialize, Serialize};
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

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
        recv: &mut FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>,
    ) -> Result<RpcRequest, RpcError> {
        let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
        Ok(serde_json::from_slice(&bytes)?)
    }

    async fn write_response(
        send: &mut FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>,
        response: &RpcResponse,
    ) -> Result<(), RpcError> {
        let bytes = serde_json::to_vec(response)?;
        send.send(bytes.into()).await?;
        Ok(())
    }
}

#[async_trait]
#[rustfmt::skip]
impl ProtocolHandler for RpcHandler {
    async fn handle(
        &self, 
        peer: &PeerId, 
        send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError> {
        let mut framed_recv = FramedRead::new(recv, LengthDelimitedCodec::new());
        let mut framed_send = FramedWrite::new(send, LengthDelimitedCodec::new());

        loop {
            match Self::read_request(&mut framed_recv).await {
                Ok(RpcRequest::Ping) => {
                    log::debug!("[RpcHandler] ping from {}", peer.id);
                    Self::write_response(&mut framed_send, &RpcResponse::Pong).await?;
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
