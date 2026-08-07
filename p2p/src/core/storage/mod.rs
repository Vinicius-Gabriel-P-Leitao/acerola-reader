//! Abstrações e implementações de persistência para o acerola-p2p (P2PStorage).
//!
//! Separação de responsabilidades:
//! - Vault: Persistência de segredos e chaves de identidade.
//! - Cache: Persistência de metadados e endereços conhecidos de peers para reconexão/bootstrapping.

use std::{collections::HashMap, sync::Arc};

use async_trait::async_trait;
use tokio::sync::RwLock;

use crate::infra::{
    error::ConnectionError,
    peer::{PeerAddr, PeerId},
};

/// Contrato principal para persistência de identidade e peers descobertos.
#[async_trait]
pub trait P2PStorage: Send + Sync {
    /// Salva a chave/seed de identidade do nó local no Vault seguro.
    async fn save_identity(&self, secret: &[u8]) -> Result<(), ConnectionError>;

    /// Carrega a chave/seed de identidade salva anteriormente no Vault.
    async fn load_identity(&self) -> Result<Option<Vec<u8>>, ConnectionError>;

    /// Salva um peer e seus dados de discagem no Cache de peers.
    async fn save_peer(&self, peer: &PeerAddr) -> Result<(), ConnectionError>;

    /// Carrega todos os peers previamente conhecidos do Cache.
    async fn load_peers(&self) -> Result<Vec<PeerAddr>, ConnectionError>;
}

/// Implementação padrão em memória do `P2PStorage` (ideal para testes ou ambientes sem disco).
#[derive(Default, Clone)]
pub struct InMemoryStorage {
    identity: Arc<RwLock<Option<Vec<u8>>>>,
    peers: Arc<RwLock<HashMap<PeerId, PeerAddr>>>,
}

impl InMemoryStorage {
    /// Instancia um novo storage em memória limpo.
    pub fn new() -> Self {
        Self::default()
    }
}

#[async_trait]
impl P2PStorage for InMemoryStorage {
    async fn save_identity(&self, secret: &[u8]) -> Result<(), ConnectionError> {
        *self.identity.write().await = Some(secret.to_vec());
        Ok(())
    }

    async fn load_identity(&self) -> Result<Option<Vec<u8>>, ConnectionError> {
        Ok(self.identity.read().await.clone())
    }

    async fn save_peer(&self, peer: &PeerAddr) -> Result<(), ConnectionError> {
        self.peers.write().await.insert(peer.id.clone(), peer.clone());
        Ok(())
    }

    async fn load_peers(&self) -> Result<Vec<PeerAddr>, ConnectionError> {
        Ok(self.peers.read().await.values().cloned().collect())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string(), device_id: None }
    }

    fn make_addr(id: &str) -> PeerAddr {
        PeerAddr { id: make_peer(id), addrs: vec![1, 2, 3] }
    }

    #[tokio::test]
    async fn in_memory_storage_identity_roundtrip() {
        let storage = InMemoryStorage::new();
        assert!(storage.load_identity().await.unwrap().is_none());

        let secret = [0x42u8; 32];
        storage.save_identity(&secret).await.unwrap();

        let loaded = storage.load_identity().await.unwrap();
        assert_eq!(loaded, Some(secret.to_vec()));
    }

    #[tokio::test]
    async fn in_memory_storage_peers_roundtrip() {
        let storage = InMemoryStorage::new();
        assert!(storage.load_peers().await.unwrap().is_empty());

        let addr1 = make_addr("node-1");
        let addr2 = make_addr("node-2");

        storage.save_peer(&addr1).await.unwrap();
        storage.save_peer(&addr2).await.unwrap();

        let loaded = storage.load_peers().await.unwrap();
        assert_eq!(loaded.len(), 2);
        assert!(loaded.contains(&addr1));
        assert!(loaded.contains(&addr2));
    }
}
