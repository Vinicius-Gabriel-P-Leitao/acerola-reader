use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
};

/// Mesmo padrão de `comic_sync_registry.rs::PendingComicSyncRegistry` — o comando Tauri
/// `query_remote_cover` grava `(comic_name, known_version)` pro peer ANTES de chamar
/// `connect()`, e `CoverBrowseOutbound::handle` consome (`take`) esse valor assim que a sessão
/// outbound começa. Só o lado outbound precisa disso — o lado inbound recebe tudo pelo próprio
/// `CoverRequest` no wire.
#[derive(Default)]
pub struct PendingCoverRequestRegistry {
    pending: Mutex<HashMap<String, (String, Option<i64>)>>,
}

impl PendingCoverRequestRegistry {
    pub fn new() -> Arc<Self> {
        Arc::new(Self::default())
    }

    pub fn set(&self, peer_id: String, comic_name: String, known_version: Option<i64>) {
        self.pending
            .lock()
            .expect("pending cover request registry mutex poisoned")
            .insert(peer_id, (comic_name, known_version));
    }

    pub fn take(&self, peer_id: &str) -> Option<(String, Option<i64>)> {
        self.pending.lock().expect("pending cover request registry mutex poisoned").remove(peer_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn take_removes_the_pending_entry() {
        let registry = PendingCoverRequestRegistry::new();
        registry.set("peer-1".to_string(), "Berserk".to_string(), Some(42));

        assert_eq!(registry.take("peer-1"), Some(("Berserk".to_string(), Some(42))));
        assert_eq!(registry.take("peer-1"), None);
    }

    #[test]
    fn take_without_a_pending_entry_returns_none() {
        let registry = PendingCoverRequestRegistry::new();
        assert_eq!(registry.take("peer-unknown"), None);
    }
}
