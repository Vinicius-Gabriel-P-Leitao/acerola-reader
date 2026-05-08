use std::sync::Arc;

use async_trait::async_trait;
use iroh::{Endpoint, EndpointAddr, EndpointId};
use tokio::io::{AsyncRead, AsyncWrite};

use super::connection::{ConnectionReader, ConnectionWriter, IrohIncoming};
use crate::{
    core::transport::{IncomingConnection, P2pTransport},
    infra::{error::ConnectionError, peer::PeerId},
};

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
        let id: EndpointId = peer.id.parse().map_err(|_| ConnectionError::PeerNotFound(PeerId { id: peer.id.clone(), device_id: None }))?;
        Ok(EndpointAddr::from(id))
    }
}

#[async_trait]
impl P2pTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        let node_id = self.endpoint.id();
        PeerId::from_public_key(node_id.to_string(), node_id.as_bytes())
    }

    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError> {
        let incoming = self.endpoint.accept().await.ok_or(ConnectionError::Shutdown)?;
        tracing::trace!(layer = "iroh_transport", "incoming connection request received");

        let conn = incoming.await?;
        let remote_id = conn.remote_id();
        let alpn = conn.alpn();

        tracing::debug!(
            layer = "iroh_transport",
            peer = %remote_id,
            alpn = ?String::from_utf8_lossy(alpn),
            "inbound connection established"
        );

        Ok(Box::new(IrohIncoming::new(
            conn.clone(),
            PeerId::from_public_key(remote_id.to_string(), remote_id.as_bytes()),
            alpn.to_vec(),
        )))
    }

    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerId,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        let addr = self.peer_to_addr(peer)?;
        tracing::debug!(
            layer = "iroh_transport",
            peer = %peer.id,
            alpn = ?String::from_utf8_lossy(alpn),
            "initiating outbound connection"
        );

        let conn = self.endpoint.connect(addr, alpn).await?;
        let (send, recv) = conn.open_bi().await?;

        tracing::trace!(
            layer = "iroh_transport",
            peer = %peer.id,
            "outbound bi-stream opened"
        );

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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::core::transport::{iroh::IrohTransportBuilder, TransportP2pBuilder};

    async fn build_transport() -> IrohTransport {
        IrohTransportBuilder::default().build(vec![b"test/proto".to_vec()]).await.unwrap()
    }

    #[tokio::test]
    async fn local_id_nao_vazio() {
        let transport = build_transport().await;
        assert!(!transport.local_id().id.is_empty());
    }

    #[tokio::test]
    async fn local_id_tem_device_id_preenchido() {
        let transport = build_transport().await;
        assert!(transport.local_id().device_id.is_some());
    }

    #[tokio::test]
    async fn mesma_seed_gera_mesmo_device_id() {
        let seed = [0x42u8; 32];
        let t1 = IrohTransportBuilder::default().seed(seed).build(vec![]).await.unwrap();
        let t2 = IrohTransportBuilder::default().seed(seed).build(vec![]).await.unwrap();
        assert_eq!(t1.local_id().device_id, t2.local_id().device_id);
    }

    #[tokio::test]
    async fn seeds_diferentes_geram_device_ids_diferentes() {
        let t1 = IrohTransportBuilder::default().seed([0x11u8; 32]).build(vec![]).await.unwrap();
        let t2 = IrohTransportBuilder::default().seed([0x22u8; 32]).build(vec![]).await.unwrap();
        assert_ne!(t1.local_id().device_id, t2.local_id().device_id);
    }

    #[tokio::test]
    async fn shutdown_sem_erro() {
        let transport = build_transport().await;
        assert!(transport.shutdown().await.is_ok());
    }
}
