use std::{collections::HashSet, path::Path, sync::Arc};

use acerola_p2p::api::{guard::TrustedPeerStore, protocol::EventEmitter};
use async_trait::async_trait;
use tokio::sync::RwLock;

use crate::callbacks::SecureBlobStore;

const PEER_TRUSTED_FIRST_TIME_EVENT: &str = "peer:trusted_first_time";
const TRUSTED_KEY: &str = "trusted";
const BLOCKED_KEY: &str = "blocked";

/// Implementação de `TrustedPeerStore` que persiste via [`SecureBlobStore`] (Kotlin,
/// `EncryptedSharedPreferences` protegida pelo Android Keystore) em vez de arquivos texto
/// (um id por linha) — mantém a confiança TOFU permanente entre reinícios do processo e entre
/// trocas de modo (local/relay), agora protegida em repouso.
///
/// Ao registrar um peer como confiável pela primeira vez, emite o evento
/// `peer:trusted_first_time` pelo mesmo canal do `P2PCallback`, já que a lib
/// (`TofuGuard`) não expõe um hook próprio para esse instante.
pub(crate) struct SecureTrustedStore {
    store: Arc<dyn SecureBlobStore>,
    trusted: RwLock<HashSet<String>>,
    blocked: HashSet<String>,
    emit: EventEmitter,
}

impl SecureTrustedStore {
    /// `legacy_dir`, se fornecido e ainda tiver `trusted.txt`/`blocked.txt` (formato antigo,
    /// texto puro com um id por linha), tem seu conteúdo migrado pro storage seguro numa única
    /// vez — sem isso, a lista de confiança TOFU existente seria perdida no primeiro update e
    /// todo peer já pareado pediria confirmação de novo.
    pub(crate) fn open(
        store: Arc<dyn SecureBlobStore>, emit: EventEmitter, legacy_dir: Option<&Path>,
    ) -> Self {
        if let Some(legacy_dir) = legacy_dir {
            Self::migrate_legacy_files(&store, legacy_dir);
        }

        let trusted = Self::read_set(&store, TRUSTED_KEY);
        let blocked = Self::read_set(&store, BLOCKED_KEY);

        Self { store, trusted: RwLock::new(trusted), blocked, emit }
    }

    fn migrate_legacy_files(store: &Arc<dyn SecureBlobStore>, legacy_dir: &Path) {
        Self::migrate_legacy_file(store, &legacy_dir.join("trusted.txt"), TRUSTED_KEY);
        Self::migrate_legacy_file(store, &legacy_dir.join("blocked.txt"), BLOCKED_KEY);
    }

    /// Só migra se o storage seguro confirmar (`Ok(None)`) que a chave está genuinamente
    /// vazia — em caso de falha real de backend (`Err`), não migra nem apaga o arquivo
    /// legado, pra não arriscar perder o único dado bom que ainda existe.
    fn migrate_legacy_file(store: &Arc<dyn SecureBlobStore>, path: &Path, key: &str) {
        match store.load_blob(key.to_string()) {
            Ok(None) => {
                if let Ok(Some(bytes)) = crate::fsutil::read_optional(path) {
                    let set: HashSet<String> = String::from_utf8_lossy(&bytes)
                        .lines()
                        .map(str::trim)
                        .filter(|line| !line.is_empty())
                        .map(str::to_string)
                        .collect();

                    match serde_json::to_vec(&set) {
                        Ok(json) => {
                            if let Err(err) = store.save_blob(key.to_string(), json) {
                                tracing::error!(layer = "storage", key, error = %err, "failed to migrate legacy file");
                                return;
                            }
                        },
                        Err(err) => {
                            tracing::error!(layer = "storage", key, error = ?err, "failed to encode migrated set");
                            return;
                        },
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

    fn read_set(store: &Arc<dyn SecureBlobStore>, key: &str) -> HashSet<String> {
        match store.load_blob(key.to_string()) {
            Ok(Some(bytes)) => serde_json::from_slice(&bytes).unwrap_or_default(),
            Ok(None) => HashSet::new(),
            Err(err) => {
                tracing::error!(layer = "storage", key, error = %err, "failed to load set, starting empty");
                HashSet::new()
            },
        }
    }

    fn write_set(&self, key: &str, set: &HashSet<String>) {
        match serde_json::to_vec(set) {
            Ok(bytes) => {
                if let Err(err) = self.store.save_blob(key.to_string(), bytes) {
                    tracing::error!(layer = "storage", key, error = %err, "failed to persist blob");
                }
            },
            Err(err) => {
                tracing::error!(layer = "storage", key, error = ?err, "failed to encode set");
            },
        }
    }
}

#[async_trait]
impl TrustedPeerStore for SecureTrustedStore {
    async fn insert(&self, id: &str) {
        let mut trusted = self.trusted.write().await;
        if trusted.contains(id) {
            return;
        }

        trusted.insert(id.to_string());
        self.write_set(TRUSTED_KEY, &trusted);
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
