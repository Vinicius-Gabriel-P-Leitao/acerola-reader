//! Stub de Transporte implementado para mockar conexões e isolar o unit-test do NetworkManager.
//!
//! Provê o `MockTransport` e a sua respectiva manivela (`MockTransportHandle`), que
//! permite injetar conexões programaticamente (como pipes duplexados na memória)
//! simulando solicitações reais que viriam da rede exterior.

use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite, DuplexStream};
use tokio::sync::{mpsc, Mutex};

use crate::error::ConnectionError;
use crate::peer::PeerId;
use crate::transport::{IncomingConnection, P2pTransport};

/// Assinatura interna que empacota as propriedades forjadas de uma nova "conexão P2P".
type InjectedConnection =
    (Vec<u8>, PeerId, Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>);

/// Conexão simulada representando um par de streams já atreladas a um nó fictício.
struct MockIncoming {
    alpn: Vec<u8>,
    peer: PeerId,
    send: Box<dyn AsyncWrite + Send + Unpin>,
    recv: Box<dyn AsyncRead + Send + Unpin>,
}

#[async_trait]
impl IncomingConnection for MockIncoming {
    fn alpn(&self) -> &[u8] {
        &self.alpn
    }

    fn peer(&self) -> &PeerId {
        &self.peer
    }

    async fn accept_bi(
        self: Box<Self>,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        Ok((self.send, self.recv))
    }
}

/// Implementador falso (Dummy) da trait `P2PTransport`.
/// O NetworkManager irá ficar suspenso esperando conexões que o handle submete pelo buffer.
pub struct MockTransport {
    rx: Mutex<mpsc::UnboundedReceiver<InjectedConnection>>,
}

/// A manivela para disparar streams pra dentro do ambiente Mocado.
pub struct MockTransportHandle {
    tx: mpsc::UnboundedSender<InjectedConnection>,
}

impl MockTransportHandle {
    /// Dispara falsamente a solicitação simulando um Remote Peer tentando parear um protocolo.
    pub fn inject(&self, alpn: &[u8], peer: PeerId, client: DuplexStream, server: DuplexStream) {
        let _ = self.tx.send((alpn.to_vec(), peer, Box::new(server), Box::new(client)));
    }
}

/// Construtor emparelhado que devolve tanto o Transporte Fictício (para injetar no Builder)
/// quanto o Handle (para ser usado pelo desenvolvedor nos scripts de teste).
pub fn mock_transport() -> (MockTransport, MockTransportHandle) {
    let (tx, rx) = mpsc::unbounded_channel();
    (MockTransport { rx: Mutex::new(rx) }, MockTransportHandle { tx })
}

#[async_trait]
impl P2pTransport for MockTransport {
    fn local_id(&self) -> PeerId {
        PeerId { id: "mock-peer".to_string() }
    }

    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError> {
        let (alpn, peer, send, recv) =
            self.rx.lock().await.recv().await.ok_or(ConnectionError::Shutdown)?;

        Ok(Box::new(MockIncoming { alpn, peer, send, recv }))
    }

    async fn open_bi(
        &self, _alpn: &[u8], _peer: &PeerId,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        Err(ConnectionError::Shutdown)
    }

    async fn shutdown(&self) -> Result<(), ConnectionError> {
        Ok(())
    }
}