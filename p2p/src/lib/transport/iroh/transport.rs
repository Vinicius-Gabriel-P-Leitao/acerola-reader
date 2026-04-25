use async_trait::async_trait;
use iroh::{Endpoint, EndpointAddr, EndpointId};
use std::sync::Arc;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::error::ConnectionError;
use crate::peer::PeerId;
use crate::transport::{IncomingConnection, P2pTransport};
use super::connection::{ConnectionReader, ConnectionWriter, IrohIncoming};

/// Interface concreta que gerencia o Endpoint UDP local e a configuração de chaves usando a suite Iroh.
pub struct IrohTransport {
    endpoint: Endpoint,
}

impl IrohTransport {
    pub(crate) fn new(endpoint: Endpoint) -> Self {
        Self { endpoint }
    }

    /// Trata a conversão sintática das Strings em NodeIds estritos nativos do iroh.
    #[rustfmt::skip]
    fn peer_to_addr(&self, peer: &PeerId) -> Result<EndpointAddr, ConnectionError> {
        let id: EndpointId = peer.id.parse().map_err(|_| ConnectionError::PeerNotFound(PeerId { id: peer.id.clone() }))?;
        Ok(EndpointAddr::from(id))
    }
}

#[async_trait]
impl P2pTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        PeerId { id: self.endpoint.id().to_string() }
    }

    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError> {
        let incoming = self.endpoint.accept().await.ok_or(ConnectionError::Shutdown)?;
        let conn = incoming.await?;

        Ok(Box::new(IrohIncoming::new(
            conn.clone(),
            PeerId { id: conn.remote_id().to_string() },
            conn.alpn().to_vec(),
        )))
    }

    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerId,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        let addr = self.peer_to_addr(peer)?;
        let conn = self.endpoint.connect(addr, alpn).await?;
        let (send, recv) = conn.open_bi().await?;

        let shared_conn = Arc::new(conn);
        Ok((
            Box::new(ConnectionWriter::new(send, Arc::clone(&shared_conn))),
            Box::new(ConnectionReader::new(recv, shared_conn)),
        ))
    }

    /// Executa o teardown forçado do componente iroh.
    ///
    /// Warn: O endpoint é compartilhado em formato Arc no backend do crate `iroh`.
    /// Desligar essa faceta pode necessitar dropar todos os componentes de leitura remanescentes.
    async fn shutdown(&self) -> Result<(), ConnectionError> {
        self.endpoint.close().await;
        Ok(())
    }
}
