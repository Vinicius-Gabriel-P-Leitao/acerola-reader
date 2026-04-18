use crate::infra::remote::p2p::peer_id::PeerId;
use std::collections::HashMap;

pub struct NetworkState {
    connected_peers: HashMap<PeerId, Vec<u8>>,
}

impl NetworkState {
    pub fn new() -> Self {
        Self { connected_peers: HashMap::new() }
    }

    pub fn connect(&mut self, peer: PeerId, alpn: Vec<u8>) {
        self.connected_peers.insert(peer, alpn);
    }

    pub fn disconnect(&mut self, peer: &PeerId) {
        self.connected_peers.remove(peer);
    }

    pub fn is_connected(&self, peer: &PeerId) -> bool {
        self.connected_peers.contains_key(peer)
    }

    pub fn peers(&self) -> &HashMap<PeerId, Vec<u8>> {
        &self.connected_peers
    }
}
