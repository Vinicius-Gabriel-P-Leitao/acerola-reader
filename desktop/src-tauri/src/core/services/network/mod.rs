use std::{collections::HashSet, sync::Arc};

use acerola_p2p::api::{
    guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
    identity::DeviceInfo,
    network::NetworkMode,
    peer::{PeerAddr, PeerIdentity},
    AcerolaP2p,
};
use async_trait::async_trait;

pub type ConnectedPeerInfo = (PeerIdentity, HashSet<Vec<u8>>, Option<DeviceInfo>);

#[async_trait]
pub trait NetworkServiceApi: Send + Sync + 'static {
    fn local_id(&self) -> Result<String, String>;
    async fn connected_peers_with_info(&self) -> Result<Vec<ConnectedPeerInfo>, String>;
    async fn switch_to_local(&self) -> Result<(), String>;
    async fn switch_to_relay(&self) -> Result<(), String>;
    async fn mode(&self) -> Result<NetworkMode, String>;
    async fn connect(&self, peer_addr: PeerAddr, alpn: Vec<u8>) -> Result<(), String>;
    async fn shutdown(&self) -> Result<(), String>;
}

pub struct NetworkService {
    node: Arc<AcerolaP2p>,
}

impl NetworkService {
    pub fn new(node: Arc<AcerolaP2p>) -> Self {
        Self { node }
    }
}

#[async_trait]
impl NetworkServiceApi for NetworkService {
    fn local_id(&self) -> Result<String, String> {
        Ok(self.node.local_id().to_string())
    }

    async fn connected_peers_with_info(&self) -> Result<Vec<ConnectedPeerInfo>, String> {
        Ok(self.node.connected_peers_with_info().await)
    }

    async fn switch_to_local(&self) -> Result<(), String> {
        let store = Arc::new(InMemoryTrustedStore::new());
        let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
        self.node.switch_guard(guard, NetworkMode::Local).await.map_err(|err| err.to_string())
    }

    async fn switch_to_relay(&self) -> Result<(), String> {
        let store = Arc::new(InMemoryTrustedStore::new());
        let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
        self.node.switch_guard(guard, NetworkMode::Relay).await.map_err(|err| err.to_string())
    }

    async fn mode(&self) -> Result<NetworkMode, String> {
        Ok(self.node.mode().await)
    }

    async fn connect(&self, peer_addr: PeerAddr, alpn: Vec<u8>) -> Result<(), String> {
        self.node.connect(peer_addr, &alpn).await.map_err(|err| err.to_string())
    }

    async fn shutdown(&self) -> Result<(), String> {
        self.node.shutdown().await.map_err(|err| err.to_string())
    }
}
