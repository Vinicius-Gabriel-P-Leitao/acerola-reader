use async_trait::async_trait;
use futures::sink::SinkExt;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use crate::infra::{
    error::messages::{connection_error::ConnectionError, rpc_error::RpcError},
    remote::p2p::{peer_id::PeerId, protocol_handler::ProtocolHandler},
};

// Tipos que representam o protocolo RPC.
// Ambos os lados sabem serializar e desserializar esses enums.
#[derive(Debug, Serialize, Deserialize)]
pub enum RpcRequest {
    Ping,
}

#[derive(Debug, Serialize, Deserialize)]
pub enum RpcResponse {
    Pong,
}

// --- Funções auxiliares compartilhadas ---
//
// FramedRead/FramedWrite são wrappers do tokio_util que adicionam
// enquadramento (framing) sobre streams de bytes brutos.
//
// O problema sem framing: TCP/QUIC são streams contínuos de bytes.
// Se você enviar dois JSONs em sequência, o receptor pode receber
// tudo junto num único chunk. O LengthDelimitedCodec resolve isso
// prefixando cada mensagem com seu tamanho em bytes.
//
// SinkExt (futures) adiciona o método .send() no FramedWrite.
// StreamExt (tokio_stream) adiciona o método .next() no FramedRead.

type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

pub type EventEmitter = Arc<dyn Fn(&str, String) + Send + Sync>;

async fn read_request(recv: &mut Recv) -> Result<RpcRequest, RpcError> {
    let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
    Ok(serde_json::from_slice(&bytes)?)
}

async fn write_request(send: &mut Writer, request: &RpcRequest) -> Result<(), RpcError> {
    let bytes = serde_json::to_vec(request)?;
    send.send(bytes.into()).await?;
    Ok(())
}

async fn read_response(recv: &mut Recv) -> Result<RpcResponse, RpcError> {
    let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
    Ok(serde_json::from_slice(&bytes)?)
}

async fn write_response(send: &mut Writer, response: &RpcResponse) -> Result<(), RpcError> {
    let bytes = serde_json::to_vec(response)?;
    send.send(bytes.into()).await?;
    Ok(())
}

// --- Servidor ---
// Papel: espera mensagens, responde. Registrado no inbound do manager.

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
            match read_request(&mut framed_recv).await {
                Ok(RpcRequest::Ping) => {
                    log::debug!("[RpcServer] ping from {}", peer.id);
                    (self.emit)("rpc:ping_received", peer.id.clone());

                    write_response(&mut framed_send, &RpcResponse::Pong).await?;
                    (self.emit)("rpc:pong_sent", peer.id.clone());
                },
                Err(_) => break,
            }
        }

        Ok(())
    }
}

// --- Cliente ---
// Papel: fala primeiro (envia Ping), depois entra no mesmo loop bidirecional.
// Registrado no outbound do manager.

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

        // Cliente fala primeiro
        write_request(&mut framed_send, &RpcRequest::Ping).await?;
        (self.emit)("rpc:ping_sent", peer.id.clone());

        // Lê o Pong da resposta inicial
        match read_response(&mut framed_recv).await {
            Ok(RpcResponse::Pong) => {
                log::debug!("[RpcClient] pong from {}", peer.id);
                (self.emit)("rpc:pong_received", peer.id.clone());
            },
            Err(_) => return Ok(()),
        }

        // Depois fica no mesmo loop bidirecional do servidor
        loop {
            match read_request(&mut framed_recv).await {
                Ok(RpcRequest::Ping) => {
                    write_response(&mut framed_send, &RpcResponse::Pong).await?;
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
    async fn ping_serializa_e_deserializa() {
        let request = RpcRequest::Ping;
        let bytes = serde_json::to_vec(&request).unwrap();
        let decoded: RpcRequest = serde_json::from_slice(&bytes).unwrap();
        assert!(matches!(decoded, RpcRequest::Ping));
    }

    #[tokio::test]
    async fn pong_serializa_e_deserializa() {
        let response = RpcResponse::Pong;
        let bytes = serde_json::to_vec(&response).unwrap();
        let decoded: RpcResponse = serde_json::from_slice(&bytes).unwrap();
        assert!(matches!(decoded, RpcResponse::Pong));
    }
}
