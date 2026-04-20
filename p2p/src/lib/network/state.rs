use std::collections::{HashMap, HashSet};

use crate::peer::PeerId;

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

#[cfg(test)]
mod tests {
    use super::*;

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string() }
    }

    #[test]
    fn peer_conectado_aparece_no_state() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/rpc".to_vec());
        assert!(state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn peer_desconectado_some_do_state() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/rpc".to_vec());
        state.disconnect(&make_peer("peer-1"), b"acerola/rpc");
        assert!(!state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn peer_permanece_conectado_apos_remover_um_de_dois_alpns() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/rpc".to_vec());
        state.connect(make_peer("peer-1"), b"acerola/blob".to_vec());

        state.disconnect(&make_peer("peer-1"), b"acerola/rpc");

        assert!(state.is_connected(&make_peer("peer-1")));
        assert!(!state.is_connected_on(&make_peer("peer-1"), b"acerola/rpc"));
        assert!(state.is_connected_on(&make_peer("peer-1"), b"acerola/blob"));
    }

    #[test]
    fn peer_removido_quando_todos_alpns_desconectam() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/rpc".to_vec());
        state.connect(make_peer("peer-1"), b"acerola/blob".to_vec());

        state.disconnect(&make_peer("peer-1"), b"acerola/rpc");
        state.disconnect(&make_peer("peer-1"), b"acerola/blob");

        assert!(!state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn disconnect_de_alpn_inexistente_nao_afeta_outros() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/rpc".to_vec());
        state.disconnect(&make_peer("peer-1"), b"acerola/unknown");
        assert!(state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn modo_inicial_e_local() {
        let state = NetworkState::new();
        assert!(matches!(state.mode(), NetworkMode::Local));
    }
}
