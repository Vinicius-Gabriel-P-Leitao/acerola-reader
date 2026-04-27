use acerola_p2p::api::{
    guard::{open_guard, Guard},
    network::NetworkMode,
    peer::PeerIdentity,
    AcerolaP2P,
};

use crate::data::remote::p2p::token_guard::token_guard;
use std::{collections::{HashMap, HashSet}, sync::Arc};
use tokio::sync::RwLock;

pub struct NetworkService {
    node: Arc<AcerolaP2P>,
    mode: RwLock<NetworkMode>,
}

impl NetworkService {
    pub fn new(node: Arc<AcerolaP2P>) -> Self {
        Self { node, mode: RwLock::new(NetworkMode::Local) }
    }

    pub fn local_id(&self) -> String {
        self.node.local_id().to_string()
    }

    pub async fn connected_peers(&self) -> HashMap<PeerIdentity, HashSet<Vec<u8>>> {
        self.node.connected_peers().await
    }

    pub async fn switch_to_local(&self) {
        let validator: Guard = Box::new(|ctx| Box::pin(open_guard(ctx)));
        let _ = self.node.switch_guard(validator, NetworkMode::Local).await;
    }

    pub async fn switch_to_relay(&self) {
        let validator: Guard = Box::new(|ctx| Box::pin(token_guard(ctx)));
        let _ = self.node.switch_guard(validator, NetworkMode::Relay).await;
    }

    pub async fn mode(&self) -> NetworkMode {
        self.mode.read().await.clone()
    }

    pub async fn connect(&self, peer_id: String, alpn: Vec<u8>) {
        let _ = self.node.connect(&peer_id, &alpn).await;
    }

    pub async fn shutdown(&self) {
        let _ = self.node.shutdown().await;
    }
}
