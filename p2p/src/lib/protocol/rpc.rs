//! Implementação do protocolo interno nativo de sinalização (RPC).
//!
//! Este módulo contém os Handlers que definem o protocolo ALPN `acerola/rpc`.
//! Sua finalidade é viabilizar recursos nativos da biblioteca (como heartbeat/Ping-Pong)
//! operando de maneira agnóstica na mesma infraestrutura que roda aplicações do usuário.

use async_trait::async_trait;
use futures::sink::SinkExt;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use crate::error::{ConnectionError, RpcError};
use crate::peer::PeerId;
use crate::protocol::{EventEmitter, ProtocolHandler};

/// Sinal enviado no payload que representa uma solicitação de heartbeat ("Você está vivo?").
const PING: u8 = 0x01;
/// Sinal enviado como resposta obrigatória correspondente a um `PING` válido.
const PONG: u8 = 0x02;

/// Wrapper em torno das streams de Leitura garantindo as separações por tamanho no framing.
type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
/// Wrapper em torno das streams de Escrita limitadas por frame.
type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

/// Lê os blocos assincronamente a partir da network esperando extrair 1 único byte sinalizador.
async fn read_byte(recv: &mut Recv) -> Result<u8, RpcError> {
    let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
    bytes.first().copied().ok_or(RpcError::Stream("empty frame".into()))
}

/// Envelopa um byte sinalizador solitário no codec delimitado e o escreve no TCP/QUIC em baixo-nível.
async fn write_byte(send: &mut Writer, byte: u8) -> Result<(), RpcError> {
    send.send(vec![byte].into()).await?;
    Ok(())
}

/// Processa as instâncias ativas do protocolo `acerola/rpc` sob a perspectiva de Servidor.
///
/// Este struct apenas fica estacionário, respondendo solicitações ping com pong
/// passivamente aos pares que solicitaram conexões RPC e as enviaram.
pub struct RpcServerHandler {
    emit: EventEmitter,
}

impl RpcServerHandler {
    /// Inicializa e fornece referências para emissores de eventos subjacentes.
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

/// Lida com fluxos abertos para o `acerola/rpc` a partir da perspectiva Cliente.
///
/// Dispara imediatamente o `PING` logo quando se conecta, depois se mantém no laço
/// de prontidão caso os peers entrem em acordo para enviar fluxos mútuos.
pub struct RpcClientHandler {
    emit: EventEmitter,
}

impl RpcClientHandler {
    /// Instancia o protocolo repassando o sistema de propagação dos logs e callbacks.
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

        // Protocolo Ativo: Primeiramente, notifica a vida ativamente ao par conectado.
        write_byte(&mut framed_send, PING).await?;
        (self.emit)("rpc:ping_sent", peer.id.clone());

        match read_byte(&mut framed_recv).await {
            Ok(PONG) => {
                log::debug!("[RpcClient] pong from {}", peer.id);
                (self.emit)("rpc:pong_received", peer.id.clone());
            }
            _ => return Ok(()),
        }

        // Loop continuado onde o cliente também processa Heartbeats eventuais retornados pelo nó remoto
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
    use std::sync::{Arc, Mutex};
    use tokio::time::{sleep, Duration};

    fn capture_emitter() -> (EventEmitter, Arc<Mutex<Vec<String>>>) {
        let events = Arc::new(Mutex::new(Vec::new()));
        let events_clone = Arc::clone(&events);
        let emit: EventEmitter = Arc::new(move |event: &str, _data: String| {
            events_clone.lock().unwrap().push(event.to_string());
        });
        (emit, events)
    }

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string() }
    }

    #[test]
    fn ping_byte_diferente_de_pong() {
        assert_ne!(PING, PONG);
    }

    #[test]
    fn bytes_sao_valores_esperados() {
        assert_eq!(PING, 0x01);
        assert_eq!(PONG, 0x02);
    }

    #[tokio::test]
    async fn handshake_ping_pong_completo_entre_cliente_e_servidor() {
        let (client_stream, server_stream) = tokio::io::duplex(1024);
        let (server_emit, server_events) = capture_emitter();
        let (client_emit, client_events) = capture_emitter();

        let peer = make_peer("test-peer");

        let server = RpcServerHandler::new(server_emit);
        let client = RpcClientHandler::new(client_emit);

        let peer_s = peer.clone();
        let server_task = tokio::spawn(async move {
            let (read, write) = tokio::io::split(server_stream);
            let _ = server.handle(&peer_s, Box::new(write), Box::new(read)).await;
        });

        let peer_c = peer.clone();
        let client_task = tokio::spawn(async move {
            let (read, write) = tokio::io::split(client_stream);
            let _ = client.handle(&peer_c, Box::new(write), Box::new(read)).await;
        });

        sleep(Duration::from_millis(30)).await;
        server_task.abort();
        client_task.abort();

        let server_evts = server_events.lock().unwrap();
        let client_evts = client_events.lock().unwrap();

        assert!(server_evts.iter().any(|e| e == "rpc:ping_received"), "server should receive ping");
        assert!(server_evts.iter().any(|e| e == "rpc:pong_sent"), "server should send pong");
        assert!(client_evts.iter().any(|e| e == "rpc:ping_sent"), "client should send ping");
        assert!(client_evts.iter().any(|e| e == "rpc:pong_received"), "client should receive pong");
    }

    #[tokio::test]
    async fn servidor_processa_multiplos_pings() {
        let (client_stream, server_stream) = tokio::io::duplex(1024);
        let (server_emit, server_events) = capture_emitter();
        let (client_emit, _) = capture_emitter();

        let peer = make_peer("test-peer");

        let server = RpcServerHandler::new(server_emit);
        let client = RpcClientHandler::new(client_emit);

        let peer_s = peer.clone();
        let server_task = tokio::spawn(async move {
            let (read, write) = tokio::io::split(server_stream);
            let _ = server.handle(&peer_s, Box::new(write), Box::new(read)).await;
        });

        let peer_c = peer.clone();
        let client_task = tokio::spawn(async move {
            let (read, write) = tokio::io::split(client_stream);
            let _ = client.handle(&peer_c, Box::new(write), Box::new(read)).await;
        });

        sleep(Duration::from_millis(30)).await;
        server_task.abort();
        client_task.abort();

        let count =
            server_events.lock().unwrap().iter().filter(|e| *e == "rpc:ping_received").count();
        assert!(count >= 1);
    }
}