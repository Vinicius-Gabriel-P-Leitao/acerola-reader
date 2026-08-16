use std::path::{Path, PathBuf};

use acerola_p2p::api::{error::P2pError, peer::PeerAddr, storage::P2PStorage};
use async_trait::async_trait;
use tokio::sync::RwLock;

use super::{decrypt, encrypt};

/// Implementação de `P2PStorage` (identidade do nó + cache de peers pareados) com
/// criptografia em repouso — ver `infra::security` pro esquema (chave mestra no
/// keyring do SO, AES-256-GCM pro resto). Layout dentro de `base_dir`:
/// - `identity.enc` — seed de identidade do nó, criptografada.
/// - `peers.enc`    — lista de `PeerAddr` conhecidos (JSON antes de criptografar).
pub struct SecureP2pStorage {
    base_dir: PathBuf,
    master_key: [u8; 32],
    peers_cache: RwLock<Vec<PeerAddr>>,
}

impl SecureP2pStorage {
    pub fn open(base_dir: impl Into<PathBuf>, master_key: [u8; 32]) -> std::io::Result<Self> {
        let base_dir = base_dir.into();
        std::fs::create_dir_all(&base_dir)?;

        let peers_cache = match std::fs::read(Self::peers_path(&base_dir)) {
            Ok(encrypted) => decrypt(&master_key, &encrypted)
                .ok()
                .and_then(|json| serde_json::from_slice(&json).ok())
                .unwrap_or_default(),
            Err(err) if err.kind() == std::io::ErrorKind::NotFound => Vec::new(),
            Err(err) => return Err(err),
        };

        Ok(Self { base_dir, master_key, peers_cache: RwLock::new(peers_cache) })
    }

    fn identity_path(base_dir: &Path) -> PathBuf {
        base_dir.join("identity.enc")
    }

    fn peers_path(base_dir: &Path) -> PathBuf {
        base_dir.join("peers.enc")
    }
}

#[async_trait]
impl P2PStorage for SecureP2pStorage {
    async fn save_identity(&self, secret: &[u8]) -> Result<(), P2pError> {
        let encrypted = encrypt(&self.master_key, secret);
        std::fs::write(Self::identity_path(&self.base_dir), encrypted)
            .map_err(|err| P2pError::StreamFailed(format!("failed to save identity: {err}")))
    }

    async fn load_identity(&self) -> Result<Option<Vec<u8>>, P2pError> {
        let encrypted = match std::fs::read(Self::identity_path(&self.base_dir)) {
            Ok(bytes) => bytes,
            Err(err) if err.kind() == std::io::ErrorKind::NotFound => return Ok(None),
            Err(err) => return Err(P2pError::StreamFailed(format!("failed to load identity: {err}"))),
        };

        decrypt(&self.master_key, &encrypted)
            .map(Some)
            .map_err(|err| P2pError::StreamFailed(format!("failed to decrypt identity: {err}")))
    }

    async fn save_peer(&self, peer: &PeerAddr) -> Result<(), P2pError> {
        let mut peers = self.peers_cache.write().await;
        match peers.iter_mut().find(|cached| cached.id == peer.id) {
            Some(cached) => *cached = peer.clone(),
            None => peers.push(peer.clone()),
        }

        let json = serde_json::to_vec(&*peers)
            .map_err(|err| P2pError::StreamFailed(format!("failed to encode peer cache: {err}")))?;
        let encrypted = encrypt(&self.master_key, &json);
        std::fs::write(Self::peers_path(&self.base_dir), encrypted)
            .map_err(|err| P2pError::StreamFailed(format!("failed to save peer cache: {err}")))
    }

    async fn load_peers(&self) -> Result<Vec<PeerAddr>, P2pError> {
        Ok(self.peers_cache.read().await.clone())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use acerola_p2p::api::peer::PeerIdentity;

    fn peer(id: &str) -> PeerAddr {
        PeerAddr { id: PeerIdentity { id: id.to_string(), device_id: None }, addrs: vec![1, 2, 3] }
    }

    #[tokio::test]
    async fn identity_roundtrip_through_encrypted_file() {
        let dir = tempfile::tempdir().unwrap();
        let storage = SecureP2pStorage::open(dir.path(), [5u8; 32]).unwrap();

        assert_eq!(storage.load_identity().await.unwrap(), None);

        storage.save_identity(b"my secret seed").await.unwrap();
        assert_eq!(storage.load_identity().await.unwrap(), Some(b"my secret seed".to_vec()));

        // O arquivo no disco não pode conter o segredo em texto puro.
        let raw = std::fs::read(dir.path().join("identity.enc")).unwrap();
        assert!(!raw.windows(b"my secret seed".len()).any(|window| window == b"my secret seed"));
    }

    #[tokio::test]
    async fn save_peer_upserts_by_id_and_persists_across_reopen() {
        let dir = tempfile::tempdir().unwrap();
        let key = [9u8; 32];

        {
            let storage = SecureP2pStorage::open(dir.path(), key).unwrap();
            storage.save_peer(&peer("peer-a")).await.unwrap();
            storage.save_peer(&peer("peer-a")).await.unwrap(); // upsert, não duplica
            storage.save_peer(&peer("peer-b")).await.unwrap();
            assert_eq!(storage.load_peers().await.unwrap().len(), 2);
        }

        // Reabre (simula restart do app) — o cache tem que vir do arquivo criptografado.
        let reopened = SecureP2pStorage::open(dir.path(), key).unwrap();
        let peers = reopened.load_peers().await.unwrap();
        assert_eq!(peers.len(), 2);
        assert!(peers.iter().any(|p| p.id.id == "peer-a"));
        assert!(peers.iter().any(|p| p.id.id == "peer-b"));
    }

    #[tokio::test]
    async fn reopen_with_wrong_key_does_not_leak_old_peers() {
        let dir = tempfile::tempdir().unwrap();
        let storage = SecureP2pStorage::open(dir.path(), [1u8; 32]).unwrap();
        storage.save_peer(&peer("secret-peer")).await.unwrap();

        // Chave errada (ex: fallback local depois de trocar de máquina) — decrypt falha,
        // e o storage deve degradar pra uma lista vazia, não travar nem vazar dado velho.
        let reopened = SecureP2pStorage::open(dir.path(), [2u8; 32]).unwrap();
        assert!(reopened.load_peers().await.unwrap().is_empty());
    }
}
