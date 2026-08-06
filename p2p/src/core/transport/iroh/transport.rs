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

        let endpoint_addr = resolve_endpoint_addr(remote_id, &incoming_addr);

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

        // Prefere o caminho selecionado; aceita qualquer outro como fallback.
        // A variável local força o iterador a ser resolvido antes de `paths` ser dropado.
        let rtt = path_list
            .iter()
            .filter(|path| path.is_selected())
            .chain(path_list.iter())
            .find_map(|path| path.rtt());
        rtt
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

/// Utilitário interno para compor o EndpointAddr a partir das informações de endereço de entrada.
fn resolve_endpoint_addr(remote_id: EndpointId, incoming_addr: &IncomingAddr) -> EndpointAddr {
    let mut endpoint_addr = EndpointAddr::new(remote_id);

    match incoming_addr {
        IncomingAddr::Ip(socket_address) => {
            endpoint_addr = endpoint_addr.with_ip_addr(*socket_address);
        },
        IncomingAddr::Relay { url: relay_url, .. } => {
            endpoint_addr = endpoint_addr.with_relay_url(relay_url.clone());
        },
        _ => {},
    }

    endpoint_addr
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

    #[test]
    fn resolve_endpoint_addr_handles_relay_url() {
        // Verifica que o branch IncomingAddr::Relay associa a URL do Relay ao EndpointAddr
        let node_id = iroh::SecretKey::generate().public();
        let relay_url: iroh::RelayUrl =
            "https://relay.example.com.".parse().expect("Relay URL válida");
        let incoming_relay_address =
            IncomingAddr::Relay { url: relay_url.clone(), endpoint_id: node_id };

        let resolved_endpoint_address = resolve_endpoint_addr(node_id, &incoming_relay_address);

        assert!(resolved_endpoint_address.relay_urls().any(|u| u == &relay_url));
        assert_eq!(resolved_endpoint_address.id, node_id);
    }

    #[test]
    fn resolve_endpoint_addr_handles_ip_address() {
        // Verifica que o branch IncomingAddr::Ip associa o SocketAddr ao EndpointAddr
        let node_id = iroh::SecretKey::generate().public();
        let socket_address: std::net::SocketAddr =
            "127.0.0.1:8080".parse().expect("SocketAddr válido");
        let incoming_ip_address = IncomingAddr::Ip(socket_address);

        let resolved_endpoint_address = resolve_endpoint_addr(node_id, &incoming_ip_address);

        assert_eq!(resolved_endpoint_address.id, node_id);
        assert!(resolved_endpoint_address.ip_addrs().any(|addr| *addr == socket_address));
    }

    #[tokio::test]
    async fn iroh_transport_latency_returns_none_for_unknown_peer() {
        let transport = build_transport().await;
        let unknown_peer = PeerId { id: "unknown-peer-id".to_string(), device_id: None };
        assert!(transport.latency(&unknown_peer).await.is_none());
        assert!(P2pTransport::latency(&transport, &unknown_peer).await.is_none());
    }
}
