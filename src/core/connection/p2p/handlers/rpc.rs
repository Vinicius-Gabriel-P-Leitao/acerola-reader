use async_trait::async_trait;
use futures::sink::SinkExt;
use std::sync::Arc;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use crate::infra::{
    error::messages::{connection_error::ConnectionError, rpc_error::RpcError},
    remote::p2p::{peer_id::PeerId, protocol_handler::ProtocolHandler},
};

const PING: u8 = 0x01;
const PONG: u8 = 0x02;

type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

pub type EventEmitter = Arc<dyn Fn(&str, String) + Send + Sync>;

async fn read_byte(recv: &mut Recv) -> Result<u8, RpcError> {
    let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
    bytes.first().copied().ok_or(RpcError::Stream("empty frame".into()))
}

async fn write_byte(send: &mut Writer, byte: u8) -> Result<(), RpcError> {
    send.send(vec![byte].into()).await?;
    Ok(())
}

pub struct RpcServerHandler {
    emit: EventEmitter,
}

impl RpcServerHandler {
    pub fn new(emit: EventEmitter) -> Self {
        Self { emit }
    }
}

#[async_trait]
impl ProtocolHandler for RpcServerHandler {
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError> {
        let mut framed_recv = FramedRead::new(recv, LengthDelimitedCodec::new());
        let mut framed_send = FramedWrite::new(send, LengthDelimitedCodec::new());

        loop {
            match read_byte(&mut framed_recv).await {
                Ok(PING) => {
                    log::debug!("[RpcServer] ping from {}", peer.id);
                    (self.emit)("rpc:ping_received", peer.id.clone());
                    write_byte(&mut framed_send, PONG).await?;
                    (self.emit)("rpc:pong_sent", peer.id.clone());
                }
                _ => break,
            }
        }

        Ok(())
    }
}

pub struct RpcClientHandler {
    emit: EventEmitter,
}

impl RpcClientHandler {
    pub fn new(emit: EventEmitter) -> Self {
        Self { emit }
    }
}

#[async_trait]
impl ProtocolHandler for RpcClientHandler {
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError> {
        let mut framed_recv = FramedRead::new(recv, LengthDelimitedCodec::new());
        let mut framed_send = FramedWrite::new(send, LengthDelimitedCodec::new());

        write_byte(&mut framed_send, PING).await?;
        (self.emit)("rpc:ping_sent", peer.id.clone());

        match read_byte(&mut framed_recv).await {
            Ok(PONG) => {
                log::debug!("[RpcClient] pong from {}", peer.id);
                (self.emit)("rpc:pong_received", peer.id.clone());
            }
            _ => return Ok(()),
        }

        loop {
            match read_byte(&mut framed_recv).await {
                Ok(PING) => {
                    write_byte(&mut framed_send, PONG).await?;
                }
                _ => break,
            }
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ping_byte_diferente_de_pong() {
        assert_ne!(PING, PONG);
    }

    #[test]
    fn bytes_sao_valores_esperados() {
        assert_eq!(PING, 0x01);
        assert_eq!(PONG, 0x02);
    }
}
