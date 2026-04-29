//! Módulo responsável por armazenar o estado instantâneo da rede.
//!
//! O `NetworkState` consolida informações sobre as topologias de rede ativas,
//! quais Peers estão atualmente conectados, e os protocolos em que eles estão trafegando.

use crate::{data::identity::device_info::DeviceInfo, infra::peer::PeerId};
use std::collections::{HashMap, HashSet};

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
            connected_peers: HashMap::new(),
            mode: NetworkMode::Local,
        }
    }

    /// Registra uma conexão ativa no estado da aplicação.
    ///
    /// # Parâmetros
    /// * `peer` - O `PeerId` remoto que se conectou.
    /// * `alpn` - O array de bytes descrevendo o identificador ALPN da stream ativada.
    pub fn connect(&mut self, peer: PeerId, alpn: Vec<u8>) {
        self.connected_peers.entry(peer).or_default().insert(alpn);
    }

    /// Acessa diretamente a tabela imutável de nós conectados.
    pub fn peers(&self) -> &HashMap<PeerId, HashSet<Vec<u8>>> {
        &self.connected_peers
    }

    /// Retorna `true` se o `peer` estiver registrado no mapa (conectado por ao menos 1 protocolo).
    pub fn is_connected(&self, peer: &PeerId) -> bool {
        self.connected_peers.contains_key(peer)
    }

    /// Retorna `true` se o nó remoto está conectado por meio de um ALPN específico.
    pub fn is_connected_on(&self, peer: &PeerId, alpn: &[u8]) -> bool {
        self.connected_peers.get(peer).map_or(false, |alpns| alpns.contains(alpn))
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
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string(), device_id: None }
    }

    #[test]
    fn peer_conectado_aparece_no_state() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/handshake/1".to_vec());
        assert!(state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn peer_desconectado_some_do_state() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/handshake/1".to_vec());
        state.disconnect(&make_peer("peer-1"), b"acerola/handshake/1");
        assert!(!state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn peer_permanece_conectado_apos_remover_um_de_dois_alpns() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/handshake/1".to_vec());
        state.connect(make_peer("peer-1"), b"acerola/blob/1".to_vec());

        state.disconnect(&make_peer("peer-1"), b"acerola/handshake/1");

        assert!(state.is_connected(&make_peer("peer-1")));
        assert!(!state.is_connected_on(&make_peer("peer-1"), b"acerola/handshake/1"));
        assert!(state.is_connected_on(&make_peer("peer-1"), b"acerola/blob/1"));
    }

    #[test]
    fn peer_removido_quando_todos_alpns_desconectam() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/handshake/1".to_vec());
        state.connect(make_peer("peer-1"), b"acerola/blob/1".to_vec());

        state.disconnect(&make_peer("peer-1"), b"acerola/handshake/1");
        state.disconnect(&make_peer("peer-1"), b"acerola/blob/1");

        assert!(!state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn disconnect_de_alpn_inexistente_nao_afeta_outros() {
        let mut state = NetworkState::new();
        state.connect(make_peer("peer-1"), b"acerola/handshake/1".to_vec());
        state.disconnect(&make_peer("peer-1"), b"acerola/unknown");
        assert!(state.is_connected(&make_peer("peer-1")));
    }

    #[test]
    fn modo_inicial_e_local() {
        let state = NetworkState::new();
        assert!(matches!(state.mode(), NetworkMode::Local));
    }

    fn make_device_info(name: &str) -> DeviceInfo {
        DeviceInfo { name: name.to_string(), os: "linux".to_string(), version: "0.0.1".to_string() }
    }

    #[test]
    fn store_device_info_armazena_e_get_recupera() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        assert_eq!(state.get_device_info(&peer).unwrap().name, "meu-pc");
    }

    #[test]
    fn get_device_info_retorna_none_para_peer_desconhecido() {
        let state = NetworkState::new();
        assert!(state.get_device_info(&make_peer("fantasma")).is_none());
    }

    #[test]
    fn store_device_info_sobrescreve_info_existente() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.store_device_info(peer.clone(), make_device_info("nome-antigo"));
        state.store_device_info(peer.clone(), make_device_info("nome-novo"));

        assert_eq!(state.get_device_info(&peer).unwrap().name, "nome-novo");
    }

    #[test]
    fn device_info_removido_quando_peer_desconecta_completamente() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), b"acerola/handshake/1".to_vec());
        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.get_device_info(&peer).is_none());
    }

    #[test]
    fn device_info_persiste_enquanto_peer_tem_alpns_ativos() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), b"acerola/handshake/1".to_vec());
        state.connect(peer.clone(), b"acerola/blob/1".to_vec());
        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.get_device_info(&peer).is_some());
    }

    #[test]
    fn device_info_removido_apos_todos_alpns_desconectarem() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), b"acerola/handshake/1".to_vec());
        state.connect(peer.clone(), b"acerola/blob/1".to_vec());
        state.store_device_info(peer.clone(), make_device_info("meu-pc"));
        state.disconnect(&peer, b"acerola/handshake/1");
        state.disconnect(&peer, b"acerola/blob/1");

        assert!(state.get_device_info(&peer).is_none());
    }

    #[test]
    fn disconnect_sem_device_info_nao_causa_erro() {
        let mut state = NetworkState::new();
        let peer = make_peer("peer-1");

        state.connect(peer.clone(), b"acerola/handshake/1".to_vec());
        state.disconnect(&peer, b"acerola/handshake/1");

        assert!(state.get_device_info(&peer).is_none());
    }
}
