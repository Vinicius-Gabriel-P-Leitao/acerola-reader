use std::collections::HashSet;
use std::sync::{Arc, Mutex};

use crate::core::guard::{BoxFuture, BoxedValidator, ConnectionContext};
use crate::infra::error::ConnectionError;

pub trait TrustedPeerStore: Send + Sync {
    fn insert(&self, id: &str);
    fn contains(&self, id: &str) -> bool;
    fn is_blocked(&self, id: &str) -> bool;
}

pub struct InMemoryTrustedStore {
    trusted: Mutex<HashSet<String>>,
    blocked: Mutex<HashSet<String>>,
}

impl InMemoryTrustedStore {
    pub fn new() -> Self {
        Self { trusted: Mutex::new(HashSet::new()), blocked: Mutex::new(HashSet::new()) }
    }

    pub fn block(&self, id: &str) {
        self.blocked.lock().unwrap().insert(id.to_string());
    }
}

impl Default for InMemoryTrustedStore {
    fn default() -> Self {
        Self::new()
    }
}

impl TrustedPeerStore for InMemoryTrustedStore {
    fn insert(&self, id: &str) {
        self.trusted.lock().unwrap().insert(id.to_string());
    }

    fn contains(&self, id: &str) -> bool {
        self.trusted.lock().unwrap().contains(id)
    }

    fn is_blocked(&self, id: &str) -> bool {
        self.blocked.lock().unwrap().contains(id)
    }
}

pub struct TofuGuard {
    store: Arc<dyn TrustedPeerStore>,
}

impl TofuGuard {
    pub fn new(store: Arc<dyn TrustedPeerStore>) -> Self {
        Self { store }
    }

    pub fn into_validator(self) -> BoxedValidator {
        let store = self.store;
        Box::new(move |ctx: &ConnectionContext<()>| {
            let id = ctx.peer_id.id.clone();
            let store = Arc::clone(&store);

            Box::pin(async move {
                if store.is_blocked(&id) {
                    return Err(ConnectionError::AuthDenied);
                }
                
                if !store.contains(&id) {
                    store.insert(&id);
                }
                Ok(())
            })
        })
    }
}
