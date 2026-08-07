//! Módulo responsável por armazenar o estado instantâneo da rede.
//!
//! O `NetworkState` consolida informações sobre as topologias de rede ativas,
//! quais Peers estão atualmente conectados, e os protocolos em que eles estão trafegando.

use std::collections::{HashMap, HashSet};

use async_trait::async_trait;
use tokio::sync::RwLock;

use crate::{
    data::{identity::device_info::DeviceInfo, protocol::DeviceInfoStore},
    infra::peer::{PeerAddr, PeerId},
};

/// Modos de operação da rede.
///
/// Define o nível atual de abstração sobre a comunicação: se o nó
/// está operando somente localmente ou se depende de conectividade externa/relays.
#[derive(Clone, Debug, PartialEq)]
pub enum NetworkMode {
    /// Modo focado em testes ou tráfego P2P estritamente em LAN.
    Local,
    /// Modo que possivelmente utiliza relays para transpor NATs em ambiente global.
    Relay,
}

/// Estrutura Thread-safe que gerencia a tabela de vizinhos na rede P2P.
///
/// Mantém internamente um mapeamento de cada `PeerId` ativo para os respectivos
/// ALPNs (protocolos de aplicação) que estão utilizando a conexão com aquele par.
pub struct NetworkState {
    /// Rastreamento de pares conectados contra seus respectivos protocolos.
    connected_peers: HashMap<PeerId, HashSet<Vec<u8>>>,
    /// Mapeamento de PeerId para o último endereço conhecido para discagem.
    peer_addresses: HashMap<PeerId, PeerAddr>,
    /// Campo de informações do dispositivo entrevistado.
    peer_device_info: HashMap<PeerId, DeviceInfo>,
    /// O modo corrente  do ambiente da rede P2P.
    mode: NetworkMode,
}

impl NetworkState {
    /// Instancia um novo estado de rede, padronizando o modo para `Local`.
    pub fn new() -> Self {
        Self {
            peer_device_info: HashMap::new(),
            peer_addresses: HashMap::new(),
            connected_peers: HashMap::new(),
            mode: NetworkMode::Local,
        }
    }

    /// Registra uma conexão ativa no estado da aplicação.
    ///
    /// # Parâmetros
    /// * `peer` - O `PeerId` remoto que se conectou.
    /// * `addr` - O `PeerAddr` remoto.
    /// * `alpn` - O array de bytes descrevendo o identificador ALPN da stream ativada.
    pub fn connect(&mut self, peer: PeerId, addr: PeerAddr, alpn: Vec<u8>) {
        self.peer_addresses.insert(peer.clone(), addr);
        self.connected_peers.entry(peer).or_default().insert(alpn);
    }

    /// Acessa diretamente a tabela imutável de nós conectados.
    pub fn peers(&self) -> &HashMap<PeerId, HashSet<Vec<u8>>> {
        &self.connected_peers
    }

    /// Retorna o endereço conhecido para um peer.
    pub fn get_addr(&self, peer: &PeerId) -> Option<&PeerAddr> {
        self.peer_addresses.get(peer)
    }

    /// Armazena ou atualiza o endereço conhecido para um peer sem alterar o estado de conexão.
    pub fn store_peer_addr(&mut self, addr: PeerAddr) {
        self.peer_addresses.insert(addr.id.clone(), addr);
    }

    /// Armazena em lote múltiplos endereços de peers no cache interno de estado.
    pub fn store_peer_addrs(&mut self, addrs: impl IntoIterator<Item = PeerAddr>) {
        addrs.into_iter().for_each(|addr| self.store_peer_addr(addr));
    }

    /// Retorna `true` se o `peer` estiver registrado no mapa (conectado por ao menos 1 protocolo).
    #[cfg(test)]
    pub fn is_connected(&self, peer: &PeerId) -> bool {
        self.connected_peers.contains_key(peer)
    }

    /// Retorna `true` se o nó remoto está conectado por meio de um ALPN específico.
    #[cfg(test)]
    pub fn is_connected_on(&self, peer: &PeerId, alpn: &[u8]) -> bool {
        self.connected_peers.get(peer).is_some_and(|alpns| alpns.contains(alpn))
    }

    /// Alterna explicitamente o modo de topologia da rede (ex: transitando de LAN para WAN/Relay).
    pub fn switch_mode(&mut self, mode: NetworkMode) {
        self.mode = mode;
    }

    /// Fornece o modo em que a rede P2P está atuando.
    pub fn mode(&self) -> &NetworkMode {
        &self.mode
    }

    /// Insere um dispositivo vinculado a um PeerId
    pub fn store_device_info(&mut self, peer: PeerId, device_info: DeviceInfo) {
        self.peer_device_info.insert(peer, device_info);
    }

    /// Retorna os dados do dispositivo por peerid
    pub fn get_device_info(&self, peer: &PeerId) -> Option<&DeviceInfo> {
        self.peer_device_info.get(peer)
    }

    /// Remove o registro de uma conexão do estado.
    ///
    /// Retira a stream com a respectiva tag ALPN associada àquele nó.
    /// Caso seja o último ALPN em uso, remove todo o estado do Peer da memória.
    pub fn disconnect(&mut self, peer: &PeerId, alpn: &[u8]) {
        if let Some(alpns) = self.connected_peers.get_mut(peer) {
            alpns.remove(alpn);

            if alpns.is_empty() {
                self.connected_peers.remove(peer);
                self.peer_device_info.remove(peer);
                self.peer_addresses.remove(peer);
            }
        }
    }
}

#[async_trait]
impl DeviceInfoStore for RwLock<NetworkState> {
    async fn store_device_info(&self, peer: PeerId, info: DeviceInfo) {
        self.write().await.store_device_info(peer, info);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string(), device_id: None }
    }

    fn make_addr(id: &str) -> PeerAddr {
        PeerAddr { id: make_peer(id), addrs: vec![] }
    }

    #[test]
    fn connected_peer_appears_in_state() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        assert!(state.is_connected(&peer));
    }

    #[test]
    fn disconnected_peer_disappears_from_state() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.disconnect(&peer, b"acerola/handshake/1");
        assert!(!state.is_connected(&peer));
    }

    #[test]
    fn peer_remains_connected_after_removing_one_of_two_alpns() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/blob/1".to_vec());

        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.is_connected(&peer));
        assert!(!state.is_connected_on(&peer, b"acerola/handshake/1"));
        assert!(state.is_connected_on(&peer, b"acerola/blob/1"));
    }

    #[test]
    fn peer_removed_when_all_alpns_disconnect() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/blob/1".to_vec());

        state.disconnect(&peer, b"acerola/handshake/1");
        state.disconnect(&peer, b"acerola/blob/1");

        assert!(!state.is_connected(&peer));
    }

    #[test]
    fn disconnect_of_nonexistent_alpn_does_not_affect_others() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.disconnect(&peer, b"acerola/unknown");
        assert!(state.is_connected(&peer));
    }

    #[test]
    fn initial_mode_is_local() {
        let state = NetworkState::new();
        assert!(matches!(state.mode(), NetworkMode::Local));
    }

    fn make_device_info(name: &str) -> DeviceInfo {
        DeviceInfo { name: name.to_string(), os: "linux".to_string(), version: "0.0.1".to_string() }
    }

    #[test]
    fn store_device_info_stores_and_get_retrieves() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        assert_eq!(state.get_device_info(&peer).unwrap().name, "meu-pc");
    }

    #[test]
    fn get_device_info_returns_none_for_unknown_peer() {
        let state = NetworkState::new();
        assert!(state.get_device_info(&make_peer("fantasma")).is_none());
    }

    #[test]
    fn store_device_info_overwrites_existing_info() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.store_device_info(peer.clone(), make_device_info("nome-antigo"));
        state.store_device_info(peer.clone(), make_device_info("nome-novo"));

        assert_eq!(state.get_device_info(&peer).unwrap().name, "nome-novo");
    }

    #[test]
    fn device_info_removed_when_peer_disconnects_completely() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.get_device_info(&peer).is_none());
    }

    #[test]
    fn device_info_persists_while_peer_has_active_alpns() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/blob/1".to_vec());
        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.get_device_info(&peer).is_some());
    }

    #[test]
    fn device_info_removed_after_all_alpns_disconnect() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/blob/1".to_vec());
        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        state.disconnect(&peer, b"acerola/handshake/1");
        state.disconnect(&peer, b"acerola/blob/1");

        assert!(state.get_device_info(&peer).is_none());
    }

    #[test]
    fn disconnect_without_device_info_does_not_cause_error() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), make_addr("peer-1"), b"acerola/handshake/1".to_vec());
        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.get_device_info(&peer).is_none());
    }
}
