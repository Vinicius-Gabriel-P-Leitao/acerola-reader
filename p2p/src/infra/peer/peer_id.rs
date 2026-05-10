//! Definição da identidade de um nó (Peer) na rede P2P.
//!
//! Este módulo provê a estrutura `PeerId`, que encapsula o identificador
//! de um nó, sendo a principal forma de referenciar destinos.

use std::fmt;

use uuid::Uuid;

/// INFO: Chave para fazer chaves derivadas deterministicas.
const ACEROLA_DEVICE_NAMESPACE: Uuid = Uuid::from_bytes(*b"acerola-p2p!dev!");

/// Representa a identidade pública de um nó na rede.
///
/// O `PeerId` contém o identificador subjacente da camada de rede (ex: Iroh PublicKey
/// formatado). Implementa traits de equivalência e hashing para uso como chave
/// no rastreamento do estado das conexões ativas.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct PeerId {
    /// Identificador único (geralmente uma string em base32).
    pub id: String,
    pub device_id: Option<String>,
}

impl PeerId {
    pub fn from_public_key(id: String, public_key_bytes: &[u8]) -> Self {
        let device_id = Uuid::new_v5(&ACEROLA_DEVICE_NAMESPACE, public_key_bytes).to_string();
        Self { id, device_id: Some(device_id) }
    }
}

impl fmt::Display for PeerId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const FAKE_KEY: &[u8] = &[0xab; 32];
    const ANOTHER_KEY: &[u8] = &[0xcd; 32];

    #[test]
    fn from_public_key_preenche_device_id() {
        let peer = PeerId::from_public_key("node-1".to_string(), FAKE_KEY);
        assert!(peer.device_id.is_some());
    }

    #[test]
    fn from_public_key_e_deterministico() {
        let a = PeerId::from_public_key("node-1".to_string(), FAKE_KEY);
        let b = PeerId::from_public_key("node-1".to_string(), FAKE_KEY);
        assert_eq!(a.device_id, b.device_id);
    }

    #[test]
    fn bytes_diferentes_geram_device_ids_diferentes() {
        let a = PeerId::from_public_key("node-1".to_string(), FAKE_KEY);
        let b = PeerId::from_public_key("node-1".to_string(), ANOTHER_KEY);
        assert_ne!(a.device_id, b.device_id);
    }

    #[test]
    fn peer_sem_bytes_tem_device_id_none() {
        let peer = PeerId { id: "node-remote".to_string(), device_id: None };
        assert!(peer.device_id.is_none());
    }

    #[test]
    fn device_id_e_uuid_v5_valido() {
        let peer = PeerId::from_public_key("node-1".to_string(), FAKE_KEY);
        let uuid = Uuid::parse_str(peer.device_id.unwrap().as_str());
        assert!(uuid.is_ok());
    }

    #[test]
    fn id_nao_afeta_device_id() {
        let a = PeerId::from_public_key("node-1".to_string(), FAKE_KEY);
        let b = PeerId::from_public_key("node-2".to_string(), FAKE_KEY);
        assert_eq!(a.device_id, b.device_id);
    }
}
