use std::{
    path::{Path, PathBuf},
    sync::Arc,
};

use acerola_p2p::api::{
    error::P2pError,
    peer::PeerAddr,
    storage::P2PStorage,
};
use async_trait::async_trait;
use tokio::sync::RwLock;

/// Implementação de `P2PStorage` persistida em arquivos dentro do diretório privado do app.
///
/// Layout dentro de `base_dir`:
/// - `identity.seed`  — 32 bytes crus da seed de identidade do nó.
/// - `peers.json`      — cache de `PeerAddr` conhecidos, para reconexão/bootstrapping.
///
/// Ambos os arquivos são escritos de forma atômica (tmp + rename) para não deixar um
/// estado corrompido caso o processo seja encerrado no meio de uma escrita.
pub(crate) struct FileP2pStorage {
    base_dir: PathBuf,
    peers_cache: RwLock<Vec<PeerAddr>>,
}

impl FileP2pStorage {
    pub(crate) fn open(base_dir: impl Into<PathBuf>) -> std::io::Result<Self> {
        let base_dir = base_dir.into();
        std::fs::create_dir_all(&base_dir)?;

        let peers_cache = match crate::fsutil::read_optional(&Self::peers_path(&base_dir))? {
            Some(bytes) => serde_json::from_slice(&bytes).unwrap_or_default(),
            None => Vec::new(),
        };

        Ok(Self { base_dir, peers_cache: RwLock::new(peers_cache) })
    }

    fn identity_path(base_dir: &Path) -> PathBuf {
        base_dir.join("identity.seed")
    }

    fn peers_path(base_dir: &Path) -> PathBuf {
        base_dir.join("peers.json")
    }
}

#[async_trait]
impl P2PStorage for FileP2pStorage {
    async fn save_identity(&self, secret: &[u8]) -> Result<(), P2pError> {
        crate::fsutil::atomic_write(&Self::identity_path(&self.base_dir), secret)
            .map_err(|err| P2pError::StartupFailed(format!("failed to save identity: {err}")))
    }

    async fn load_identity(&self) -> Result<Option<Vec<u8>>, P2pError> {
        crate::fsutil::read_optional(&Self::identity_path(&self.base_dir))
            .map_err(|err| P2pError::StartupFailed(format!("failed to load identity: {err}")))
    }

    async fn save_peer(&self, peer: &PeerAddr) -> Result<(), P2pError> {
        let mut peers = self.peers_cache.write().await;
        match peers.iter_mut().find(|cached| cached.id == peer.id) {
            Some(cached) => *cached = peer.clone(),
            None => peers.push(peer.clone()),
        }

        let bytes = serde_json::to_vec(&*peers)
            .map_err(|err| P2pError::StartupFailed(format!("failed to encode peer cache: {err}")))?;
        crate::fsutil::atomic_write(&Self::peers_path(&self.base_dir), &bytes)
            .map_err(|err| P2pError::StartupFailed(format!("failed to save peer cache: {err}")))
    }

    async fn load_peers(&self) -> Result<Vec<PeerAddr>, P2pError> {
        Ok(self.peers_cache.read().await.clone())
    }
}

/// Newtype local em volta de `Arc<FileP2pStorage>` — as regras de coerência do Rust não
/// deixam implementar uma trait estrangeira (`P2PStorage`, da `acerola-p2p`) direto pra
/// `Arc<FileP2pStorage>`, já que `Arc` também é estrangeiro (não é um tipo "fundamental"
/// como `Box`). Com esse wrapper local, dá pra passar a MESMA instância pro builder (que
/// exige posse) e manter outra referência viva no `P2PNode` pra consultar depois — ex: pra
/// listar quem já foi pareado, já que a conexão do handshake em si é só um "toque" de ~2s
/// (troca PING/PONG/DeviceInfo e fecha), não uma sessão persistente.
pub(crate) struct SharedFileP2pStorage(pub(crate) Arc<FileP2pStorage>);

#[async_trait]
impl P2PStorage for SharedFileP2pStorage {
    async fn save_identity(&self, secret: &[u8]) -> Result<(), P2pError> {
        self.0.save_identity(secret).await
    }

    async fn load_identity(&self) -> Result<Option<Vec<u8>>, P2pError> {
        self.0.load_identity().await
    }

    async fn save_peer(&self, peer: &PeerAddr) -> Result<(), P2pError> {
        self.0.save_peer(peer).await
    }

    async fn load_peers(&self) -> Result<Vec<PeerAddr>, P2pError> {
        self.0.load_peers().await
    }
}
