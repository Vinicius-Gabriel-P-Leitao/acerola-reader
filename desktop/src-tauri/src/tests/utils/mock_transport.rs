use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite, DuplexStream};
use tokio::sync::{mpsc, Mutex};

use crate::infra::{
    error::messages::connection_error::ConnectionError,
    remote::p2p::{peer_id::PeerId, transport::P2PTransport},
};

type IncomingConnection = (
    Vec<u8>,
    PeerId,
    Box<dyn AsyncWrite + Send + Unpin>,
    Box<dyn AsyncRead + Send + Unpin>,
);

/// Injeta conexões simuladas no NetworkManager sem precisar do Iroh.
///
/// O teste cria um par de streams com `tokio::io::duplex`, monta a conexão
/// e envia via `inject`. O `MockTransport::accept` entrega essa conexão ao
/// NetworkManager exatamente como o IrohTransport faria.
pub struct MockTransport {
    rx: Mutex<mpsc::UnboundedReceiver<IncomingConnection>>,
}

pub struct MockTransportHandle {
    tx: mpsc::UnboundedSender<IncomingConnection>,
}

impl MockTransportHandle {
    pub fn inject(
        &self,
        alpn: &[u8],
        peer: PeerId,
        client: DuplexStream,
        server: DuplexStream,
    ) {
        let _ = self.tx.send((
            alpn.to_vec(),
            peer,
            Box::new(server),
            Box::new(client),
        ));
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

    async fn accept(
        &self,
    ) -> Result<
        (
            Vec<u8>,
            PeerId,
            Box<dyn AsyncWrite + Send + Unpin>,
            Box<dyn AsyncRead + Send + Unpin>,
        ),
        ConnectionError,
    > {
        self.rx.lock().await.recv().await.ok_or(ConnectionError::Shutdown)
    }
    
    async fn open_bi(
        &self,
        _alpn: &[u8],
        _peer: &PeerId,
    ) -> Result<
        (
            Box<dyn AsyncWrite + Send + Unpin>,
            Box<dyn AsyncRead + Send + Unpin>,
        ),
        ConnectionError,
    > {
        Err(ConnectionError::Shutdown)
    }

    async fn shutdown(&self) -> Result<(), ConnectionError> {
        Ok(())
    }
}
