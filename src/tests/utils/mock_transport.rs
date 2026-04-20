use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite, DuplexStream};
use tokio::sync::{mpsc, Mutex};

use crate::infra::{
    error::messages::connection_error::ConnectionError,
    remote::p2p::{
        peer_id::PeerId,
        transport::{IncomingConnection, P2PTransport},
    },
};

type InjectedConnection =
    (Vec<u8>, PeerId, Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>);

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

pub struct MockTransport {
    rx: Mutex<mpsc::UnboundedReceiver<InjectedConnection>>,
}

pub struct MockTransportHandle {
    tx: mpsc::UnboundedSender<InjectedConnection>,
}

impl MockTransportHandle {
    pub fn inject(&self, alpn: &[u8], peer: PeerId, client: DuplexStream, server: DuplexStream) {
        let _ = self.tx.send((alpn.to_vec(), peer, Box::new(server), Box::new(client)));
    }
}

pub fn mock_transport() -> (MockTransport, MockTransportHandle) {
    let (tx, rx) = mpsc::unbounded_channel();
    (MockTransport { rx: Mutex::new(rx) }, MockTransportHandle { tx })
}

#[async_trait]
impl P2PTransport for MockTransport {
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
