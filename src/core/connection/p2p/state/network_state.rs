use std::collections::{HashMap, HashSet};

use crate::infra::remote::p2p::peer_id::PeerId;

#[derive(Clone, Debug)]
pub enum NetworkMode {
    Local,
    Relay,
}

pub struct NetworkState {
    connected_peers: HashMap<PeerId, HashSet<Vec<u8>>>,
    mode: NetworkMode,
}

impl NetworkState {
    pub fn new() -> Self {
        Self { connected_peers: HashMap::new(), mode: NetworkMode::Local }
    }

    pub fn connect(&mut self, peer: PeerId, alpn: Vec<u8>) {
        self.connected_peers.entry(peer).or_default().insert(alpn);
    }

    pub fn disconnect(&mut self, peer: &PeerId, alpn: &[u8]) {
        if let Some(alpns) = self.connected_peers.get_mut(peer) {
            alpns.remove(alpn);
            if alpns.is_empty() {
                self.connected_peers.remove(peer);
            }
        }
    }

    pub fn is_connected(&self, peer: &PeerId) -> bool {
        self.connected_peers.contains_key(peer)
    }

    pub fn is_connected_on(&self, peer: &PeerId, alpn: &[u8]) -> bool {
        self.connected_peers.get(peer).map_or(false, |alpns| alpns.contains(alpn))
    }

    pub fn peers(&self) -> &HashMap<PeerId, HashSet<Vec<u8>>> {
        &self.connected_peers
    }

    pub fn switch_mode(&mut self, mode: NetworkMode) {
        self.mode = mode;
    }

    pub fn mode(&self) -> &NetworkMode {
        &self.mode
    }
}
