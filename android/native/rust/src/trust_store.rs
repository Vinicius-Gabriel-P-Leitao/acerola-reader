use std::{
    collections::HashSet,
    path::{Path, PathBuf},
};

use acerola_p2p::api::{
    guard::TrustedPeerStore,
    protocol::EventEmitter,
};
use async_trait::async_trait;
use tokio::sync::RwLock;

const PEER_TRUSTED_FIRST_TIME_EVENT: &str = "peer:trusted_first_time";

/// Implementação de `TrustedPeerStore` persistida em arquivos texto (um id por linha)
/// dentro do diretório privado do app — mantém a confiança TOFU permanente entre
/// reinícios do processo e entre trocas de modo (local/relay).
///
/// Ao registrar um peer como confiável pela primeira vez, emite o evento
/// `peer:trusted_first_time` pelo mesmo canal do `P2PCallback`, já que a lib
/// (`TofuGuard`) não expõe um hook próprio para esse instante.
pub(crate) struct FileTrustedStore {
    trusted_path: PathBuf,
    trusted: RwLock<HashSet<String>>,
    blocked: HashSet<String>,
    emit: EventEmitter,
}

impl FileTrustedStore {
    pub(crate) fn open(base_dir: impl AsRef<Path>, emit: EventEmitter) -> std::io::Result<Self> {
        let base_dir = base_dir.as_ref();
        std::fs::create_dir_all(base_dir)?;

        let trusted_path = base_dir.join("trusted.txt");
        let blocked_path = base_dir.join("blocked.txt");

        Ok(Self {
            trusted: RwLock::new(Self::read_set(&trusted_path)?),
            blocked: Self::read_set(&blocked_path)?,
            trusted_path,
            emit,
        })
    }

    fn read_set(path: &Path) -> std::io::Result<HashSet<String>> {
        Ok(match crate::fsutil::read_optional(path)? {
            Some(bytes) => String::from_utf8_lossy(&bytes)
                .lines()
                .map(str::trim)
                .filter(|line| !line.is_empty())
                .map(str::to_string)
                .collect(),
            None => HashSet::new(),
        })
    }

    fn write_set(path: &Path, set: &HashSet<String>) -> std::io::Result<()> {
        let contents = set.iter().cloned().collect::<Vec<_>>().join("\n");
        crate::fsutil::atomic_write(path, contents.as_bytes())
    }
}

#[async_trait]
impl TrustedPeerStore for FileTrustedStore {
    async fn insert(&self, id: &str) {
        let mut trusted = self.trusted.write().await;
        if trusted.contains(id) {
            return;
        }

        trusted.insert(id.to_string());
        if let Err(err) = Self::write_set(&self.trusted_path, &trusted) {
            tracing::error!(layer = "storage", error = ?err, "failed to persist trusted peer");
        }
        drop(trusted);

        (self.emit)(PEER_TRUSTED_FIRST_TIME_EVENT, id.to_string());
    }

    async fn contains(&self, id: &str) -> bool {
        self.trusted.read().await.contains(id)
    }

    async fn is_blocked(&self, id: &str) -> bool {
        self.blocked.contains(id)
    }
}
