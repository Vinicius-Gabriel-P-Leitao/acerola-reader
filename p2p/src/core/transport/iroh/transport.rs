use std::{collections::HashMap, sync::Arc};

use async_trait::async_trait;
use iroh::{
    endpoint::{Connection, IncomingAddr},
    Endpoint, EndpointAddr, EndpointId, Watcher,
};
use tokio::{
    io::{AsyncRead, AsyncWrite},
    sync::RwLock,
};

use super::connection::{ConnectionReader, ConnectionWriter, IrohIncoming};
use crate::{
    core::transport::{IncomingConnection, P2pTransport},
    infra::{
        error::ConnectionError,
        peer::{PeerAddr, PeerId},
    },
};

/// Interface concreta que gerencia o Endpoint UDP local e a configuração de chaves usando a suite Iroh.
pub struct IrohTransport {
    endpoint: Endpoint,
    connections: Arc<RwLock<HashMap<PeerId, Connection>>>,
}

impl IrohTransport {
    pub(crate) fn new(endpoint: Endpoint) -> Self {
        Self { endpoint, connections: Arc::new(RwLock::new(HashMap::new())) }
    }

    /// Trata a conversão sintática das Strings em NodeIds estritos nativos do iroh.
    #[rustfmt::skip]
    fn peer_to_addr(&self, peer: &PeerId) -> Result<EndpointAddr, ConnectionError> {
        let id: EndpointId = peer.id.parse().map_err(|_| ConnectionError::PeerNotFound(PeerId { id: peer.id.clone(), device_id: None }))?;
        Ok(EndpointAddr::from(id))
    }

    /// Converte um ID nativo do Iroh para o PeerId da nossa abstração.
    fn to_peer_id(&self, node_id: EndpointId) -> PeerId {
        PeerId::from_public_key(node_id.to_string(), node_id.as_bytes())
    }

    /// Converte um par ID+Endereço do Iroh para o PeerAddr da nossa abstração.
    fn to_peer_addr(&self, node_id: EndpointId, addr: EndpointAddr) -> PeerAddr {
        PeerAddr {
            id: self.to_peer_id(node_id),
            addrs: serde_json::to_vec(&addr).expect("EndpointAddr serialization failed"),
        }
    }

    pub async fn latency(&self, peer: &PeerId) -> Option<std::time::Duration> {
        let id: EndpointId = peer.id.parse().ok()?;
        let info = self.endpoint.remote_info(id).await?;
        let _ = info.addrs();
        None
    }
}

#[async_trait]
impl P2pTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        self.to_peer_id(self.endpoint.id())
    }

    fn local_addr(&self) -> Result<PeerAddr, ConnectionError> {
        Ok(self.to_peer_addr(self.endpoint.id(), self.endpoint.addr()))
    }

    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError> {
        let incoming = self.endpoint.accept().await.ok_or(ConnectionError::Shutdown)?;
        let incoming_addr = incoming.remote_addr();

        tracing::trace!(layer = "iroh_transport", "incoming connection request received");

        let conn = incoming.await?;
        let remote_id = conn.remote_id();
        let alpn = conn.alpn();

        let mut endpoint_addr = EndpointAddr::new(remote_id);

        match incoming_addr {
            IncomingAddr::Ip(addr) => {
                endpoint_addr = endpoint_addr.with_ip_addr(addr);
            },
            IncomingAddr::Relay { url, .. } => {
                endpoint_addr = endpoint_addr.with_relay_url(url);
            },
            _ => {},
        }

        let peer = self.to_peer_id(remote_id);
        let addr = self.to_peer_addr(remote_id, endpoint_addr);

        self.connections.write().await.insert(peer.clone(), conn.clone());

        tracing::debug!(
            peer = %remote_id,
            layer = "iroh_transport",
            alpn = ?String::from_utf8_lossy(alpn),
            "inbound connection established"
        );

        Ok(Box::new(IrohIncoming::new(conn.clone(), peer, addr, alpn.to_vec())))
    }

    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerAddr,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        let addr = if peer.addrs.is_empty() {
            self.peer_to_addr(&peer.id)?
        } else {
            serde_json::from_slice(&peer.addrs)
                .map_err(|_| ConnectionError::PeerNotFound(peer.id.clone()))?
        };

        tracing::debug!(
            peer = %peer.id,
            layer = "iroh_transport",
            alpn = ?String::from_utf8_lossy(alpn),
            "initiating outbound connection"
        );

        let conn = self.endpoint.connect(addr, alpn).await?;
        self.connections.write().await.insert(peer.id.clone(), conn.clone());

        let (send, recv) = conn.open_bi().await?;

        tracing::trace!(
            peer = %peer.id,
            layer = "iroh_transport",
            "outbound bi-stream opened"
        );

        let shared_conn = Arc::new(conn);

        Ok((
            Box::new(ConnectionWriter::new(send, Arc::clone(&shared_conn))),
            Box::new(ConnectionReader::new(recv, shared_conn)),
        ))
    }

    async fn latency(&self, peer: &PeerId) -> Option<std::time::Duration> {
        let conn = {
            let guard = self.connections.read().await;
            guard.get(peer).cloned()
        }?;

        if conn.close_reason().is_some() {
            self.connections.write().await.remove(peer);
            return None;
        }

        let paths = conn.paths();
        let path_list = paths.peek();

        if let Some(selected_path) = path_list.iter().find(|p| p.is_selected()) {
            if let Some(rtt) = selected_path.rtt() {
                return Some(rtt);
            }
        }

        for path in path_list.iter() {
            if let Some(rtt) = path.rtt() {
                return Some(rtt);
            }
        }

        None
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
    async fn local_id_not_empty() {
        let transport = build_transport().await;
        assert!(!transport.local_id().id.is_empty());
    }

    #[tokio::test]
    async fn local_id_has_populated_device_id() {
        let transport = build_transport().await;
        assert!(transport.local_id().device_id.is_some());
    }

    #[tokio::test]
    async fn same_seed_generates_same_device_id() {
        let seed = [0x42u8; 32];
        let t1 = IrohTransportBuilder::default().seed(seed).build(vec![]).await.unwrap();
        let t2 = IrohTransportBuilder::default().seed(seed).build(vec![]).await.unwrap();
        assert_eq!(t1.local_id().device_id, t2.local_id().device_id);
    }

    #[tokio::test]
    async fn different_seeds_generate_different_device_ids() {
        let t1 = IrohTransportBuilder::default().seed([0x11u8; 32]).build(vec![]).await.unwrap();
        let t2 = IrohTransportBuilder::default().seed([0x22u8; 32]).build(vec![]).await.unwrap();
        assert_ne!(t1.local_id().device_id, t2.local_id().device_id);
    }

    #[tokio::test]
    async fn shutdown_without_error() {
        let transport = build_transport().await;
        assert!(transport.shutdown().await.is_ok());
    }
}
