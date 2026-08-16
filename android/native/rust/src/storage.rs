use std::{path::Path, sync::Arc};

use acerola_p2p::api::{error::P2pError, peer::PeerAddr, storage::P2PStorage};
use async_trait::async_trait;
use tokio::sync::RwLock;

use crate::callbacks::SecureBlobStore;

const IDENTITY_KEY: &str = "identity";
const PEERS_KEY: &str = "peers";

/// Implementação de `P2PStorage` que persiste via [`SecureBlobStore`] (Kotlin,
/// `EncryptedSharedPreferences` protegida pelo Android Keystore) em vez de arquivos em texto
/// puro — ver `callbacks::SecureBlobStore`. Chaves: `"identity"` (seed do nó) e `"peers"`
/// (cache de `PeerAddr` conhecidos, codificado como JSON antes de virar blob).
pub(crate) struct SecureP2pStorage {
    store: Arc<dyn SecureBlobStore>,
    peers_cache: RwLock<Vec<PeerAddr>>,
}

impl SecureP2pStorage {
    /// `legacy_dir`, se fornecido e ainda tiver `identity.seed`/`peers.json` (formato antigo,
    /// texto puro, de antes da migração pro `SecureBlobStore`), tem seu conteúdo migrado pro
    /// storage seguro numa única vez — sem isso, quem já tinha pareado dispositivos perderia a
    /// identidade P2P atual no primeiro update e precisaria re-parear tudo do zero.
    pub(crate) fn open(store: Arc<dyn SecureBlobStore>, legacy_dir: Option<&Path>) -> Self {
        if let Some(legacy_dir) = legacy_dir {
            Self::migrate_legacy_files(&store, legacy_dir);
        }

        // Falha real de backend na carga inicial do cache de peers não é fatal (o node ainda
        // sobe, só sem os endereços conhecidos) — mas precisa aparecer no log, não virar
        // silenciosamente "nenhum peer conhecido".
        let peers_cache = match store.load_blob(PEERS_KEY.to_string()) {
            Ok(Some(bytes)) => serde_json::from_slice(&bytes).unwrap_or_default(),
            Ok(None) => Vec::new(),
            Err(err) => {
                tracing::error!(layer = "storage", error = %err, "failed to load cached peers, starting empty");
                Vec::new()
            },
        };

        Self { store, peers_cache: RwLock::new(peers_cache) }
    }

    fn migrate_legacy_files(store: &Arc<dyn SecureBlobStore>, legacy_dir: &Path) {
        Self::migrate_legacy_file(store, &legacy_dir.join("identity.seed"), IDENTITY_KEY);
        Self::migrate_legacy_file(store, &legacy_dir.join("peers.json"), PEERS_KEY);
    }

    /// Só migra se o storage seguro confirmar (`Ok(None)`) que a chave está genuinamente
    /// vazia — em caso de falha real de backend (`Err`), não migra nem apaga o arquivo
    /// legado, pra não arriscar perder o único dado bom que ainda existe.
    fn migrate_legacy_file(store: &Arc<dyn SecureBlobStore>, path: &Path, key: &str) {
        match store.load_blob(key.to_string()) {
            Ok(None) => {
                if let Ok(Some(bytes)) = crate::fsutil::read_optional(path) {
                    if let Err(err) = store.save_blob(key.to_string(), bytes) {
                        tracing::error!(layer = "storage", key, error = %err, "failed to migrate legacy file");
                        return;
                    }
                }
                std::fs::remove_file(path).ok();
            },
            Ok(Some(_)) => {
                // Já migrado numa execução anterior — só limpa o arquivo antigo.
                std::fs::remove_file(path).ok();
            },
            Err(err) => {
                tracing::error!(layer = "storage", key, error = %err, "secure store unavailable, skipping legacy migration");
            },
        }
    }
}

#[async_trait]
impl P2PStorage for SecureP2pStorage {
    async fn save_identity(&self, secret: &[u8]) -> Result<(), P2pError> {
        self.store
            .save_blob(IDENTITY_KEY.to_string(), secret.to_vec())
            .map_err(|err| P2pError::StartupFailed(format!("failed to save identity: {err}")))
    }

    async fn load_identity(&self) -> Result<Option<Vec<u8>>, P2pError> {
        self.store
            .load_blob(IDENTITY_KEY.to_string())
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

        self.store
            .save_blob(PEERS_KEY.to_string(), bytes)
            .map_err(|err| P2pError::StartupFailed(format!("failed to save peer cache: {err}")))
    }

    async fn load_peers(&self) -> Result<Vec<PeerAddr>, P2pError> {
        Ok(self.peers_cache.read().await.clone())
    }
}

/// Newtype local em volta de `Arc<SecureP2pStorage>` — as regras de coerência do Rust não
/// deixam implementar uma trait estrangeira (`P2PStorage`, da `acerola-p2p`) direto pra
/// `Arc<SecureP2pStorage>`, já que `Arc` também é estrangeiro (não é um tipo "fundamental"
/// como `Box`). Com esse wrapper local, dá pra passar a MESMA instância pro builder (que
/// exige posse) e manter outra referência viva no `P2PNode` pra consultar depois — ex: pra
/// listar quem já foi pareado, já que a conexão do handshake em si é só um "toque" de ~2s
/// (troca PING/PONG/DeviceInfo e fecha), não uma sessão persistente.
pub(crate) struct SharedSecureP2pStorage(pub(crate) Arc<SecureP2pStorage>);

#[async_trait]
impl P2PStorage for SharedSecureP2pStorage {
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
