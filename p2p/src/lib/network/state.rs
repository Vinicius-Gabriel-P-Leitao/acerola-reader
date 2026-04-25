//! Módulo responsável por armazenar o estado instantâneo da rede.
//!
//! O `NetworkState` consolida informações sobre as topologias de rede ativas,
//! quais Peers estão atualmente conectados, e os protocolos em que eles estão trafegando.

use std::collections::{HashMap, HashSet};

use crate::peer::PeerId;

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
    /// O modo corrente do ambiente da rede P2P.
    mode: NetworkMode,
}

impl NetworkState {
    /// Instancia um novo estado de rede, padronizando o modo para `Local`.
    pub fn new() -> Self {
        Self { connected_peers: HashMap::new(), mode: NetworkMode::Local }
    }

    /// Registra uma conexão ativa no estado da aplicação.
    ///
    /// # Parâmetros
    /// * `peer` - O `PeerId` remoto que se conectou.
    /// * `alpn` - O array de bytes descrevendo o identificador ALPN da stream ativada.
    pub fn connect(&mut self, peer: PeerId, alpn: Vec<u8>) {
        self.connected_peers.entry(peer).or_default().insert(alpn);
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
            }
        }
    }

    /// Retorna `true` se o `peer` estiver registrado no mapa (conectado por ao menos 1 protocolo).
    pub fn is_connected(&self, peer: &PeerId) -> bool {
        self.connected_peers.contains_key(peer)
    }

    /// Retorna `true` se o nó remoto está conectado por meio de um ALPN específico.
    pub fn is_connected_on(&self, peer: &PeerId, alpn: &[u8]) -> bool {
        self.connected_peers.get(peer).map_or(false, |alpns| alpns.contains(alpn))
    }

    /// Acessa diretamente a tabela imutável de nós conectados.
    pub fn peers(&self) -> &HashMap<PeerId, HashSet<Vec<u8>>> {
        &self.connected_peers
    }

    /// Alterna explicitamente o modo de topologia da rede (ex: transitando de LAN para WAN/Relay).
    pub fn switch_mode(&mut self, mode: NetworkMode) {
        self.mode = mode;
    }

    /// Fornece o modo em que a rede P2P está atuando.
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