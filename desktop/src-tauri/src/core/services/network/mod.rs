use acerola_p2p::api::{
    guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
    identity::DeviceInfo,
    network::NetworkMode,
    peer::PeerIdentity,
    AcerolaP2p,
};
use std::{collections::HashSet, sync::Arc};

pub struct NetworkService {
    node: Arc<AcerolaP2p>,
}

impl NetworkService {
    pub fn new(node: Arc<AcerolaP2p>) -> Self {
        Self { node }
    }

    pub fn local_id(&self) -> String {
        self.node.local_id().to_string()
    }

    pub async fn connected_peers_with_info(
        &self,
    ) -> Vec<(PeerIdentity, HashSet<Vec<u8>>, Option<DeviceInfo>)> {
        self.node.connected_peers_with_info().await
    }

    pub async fn switch_to_local(&self) {
        let store = Arc::new(InMemoryTrustedStore::new());
        let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
        let _ = self.node.switch_guard(guard, NetworkMode::Local).await;
    }

    pub async fn switch_to_relay(&self) {
        let store = Arc::new(InMemoryTrustedStore::new());
        let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
        let _ = self.node.switch_guard(guard, NetworkMode::Relay).await;
    }

    pub async fn mode(&self) -> NetworkMode {
        self.node.mode().await
    }

    pub async fn connect(&self, peer_id: String, alpn: Vec<u8>) {
        let _ = self.node.connect(&peer_id, &alpn).await;
    }

    pub async fn shutdown(&self) {
        let _ = self.node.shutdown().await;
    }
}
