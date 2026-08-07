use std::{
    collections::HashSet,
    sync::{Arc, Mutex},
};

use crate::{
    core::guard::{BoxedValidator, ConnectionContext},
    infra::error::ConnectionError,
};

/// Contrato para armazenamento e consulta de peers confiáveis e bloqueados.
///
/// Implementar esta trait permite controlar onde e como os dados de confiança
/// são persistidos — em memória, banco de dados, arquivo, etc.
/// A lib fornece [`InMemoryTrustedStore`] como implementação padrão.
pub trait TrustedPeerStore: Send + Sync {
    /// Registra o `id` como um peer confiável.
    fn insert(&self, id: &str);

    /// Retorna `true` se o `id` já foi registrado como confiável.
    fn contains(&self, id: &str) -> bool;

    /// Retorna `true` se o `id` está na lista de bloqueados.
    fn is_blocked(&self, id: &str) -> bool;
}

/// Implementação padrão de [`TrustedPeerStore`] com armazenamento em memória.
///
/// Mantém dois conjuntos independentes — peers confiáveis e peers bloqueados —
/// protegidos por `Mutex` para acesso seguro em múltiplas tasks Tokio simultaneamente.
///
/// Os dados são perdidos ao encerrar o processo. Para persistência entre sessões,
/// implemente [`TrustedPeerStore`] com um backend de sua escolha (SQLite, arquivo, etc).
pub struct InMemoryTrustedStore {
    trusted: Mutex<HashSet<String>>,
    blocked: Mutex<HashSet<String>>,
}

impl InMemoryTrustedStore {
    /// Cria uma nova store vazia, sem peers confiáveis nem bloqueados.
    pub fn new() -> Self {
        Self { trusted: Mutex::new(HashSet::new()), blocked: Mutex::new(HashSet::new()) }
    }

    /// Adiciona o `id` à lista de bloqueados.
    ///
    /// Peers bloqueados são rejeitados pelo [`TofuGuard`] antes de qualquer verificação
    /// de confiança, mesmo que o `id` também esteja na lista de confiáveis.
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

/// Guard com política TOFU (Trust On First Use) — confiar no primeiro uso.
///
/// Modela o comportamento do SSH na primeira conexão: peers desconhecidos são
/// automaticamente registrados e aceitos na primeira vez. Nas conexões seguintes,
/// a presença no store é suficiente para permitir o acesso.
///
/// A lógica de decisão segue esta ordem de prioridade:
/// 1. Peer está na blocklist → [`ConnectionError::AuthDenied`]
/// 2. Peer já é confiado → permite
/// 3. Peer desconhecido → registra e permite (TOFU)
///
/// # Exemplo
///
/// ```rust
/// use std::sync::Arc;
///
/// use acerola_p2p::api::guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore};
///
/// let store = Arc::new(InMemoryTrustedStore::new());
/// store.block("peer-malicioso");
///
/// let validator =
///     TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator();
/// ```
pub struct TofuGuard {
    store: Arc<dyn TrustedPeerStore>,
}

impl TofuGuard {
    /// Cria um novo `TofuGuard` com a store de confiança fornecida.
    pub fn new(store: Arc<dyn TrustedPeerStore>) -> Self {
        Self { store }
    }

    /// Consome o guard e retorna um [`BoxedValidator`] pronto para uso no builder.
    pub fn into_validator(self) -> BoxedValidator {
        let store = self.store;
        Box::new(move |ctx: &ConnectionContext<()>| {
            let id = ctx.peer_id.id.clone();
            let store = Arc::clone(&store);

            Box::pin(async move {
                if store.is_blocked(&id) {
                    return Err(ConnectionError::AuthDenied("peer is blocked".into()));
                }

                if !store.contains(&id) {
                    store.insert(&id);
                }
                Ok(())
            })
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::infra::peer::PeerId;

    fn make_store() -> Arc<InMemoryTrustedStore> {
        Arc::new(InMemoryTrustedStore::new())
    }

    fn make_ctx(id: &str) -> ConnectionContext<()> {
        ConnectionContext { peer_id: PeerId { id: id.to_string(), device_id: None }, data: () }
    }

    #[tokio::test]
    async fn unknown_peer_is_accepted_and_registered() {
        let store = make_store();
        let validator =
            TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator();

        assert!(validator(&make_ctx("novo-peer")).await.is_ok());
        assert!(store.contains("novo-peer"));
    }

    #[tokio::test]
    async fn already_known_peer_is_accepted() {
        let store = make_store();
        store.insert("peer-antigo");
        let validator =
            TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator();

        assert!(validator(&make_ctx("peer-antigo")).await.is_ok());
    }

    #[tokio::test]
    async fn blocked_peer_is_rejected() {
        let store = make_store();
        store.block("peer-malicioso");
        let validator =
            TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator();

        let result = validator(&make_ctx("peer-malicioso")).await;
        assert!(matches!(result, Err(ConnectionError::AuthDenied(_))));
    }

    #[tokio::test]
    async fn blocked_peer_is_not_inserted_into_trusted() {
        let store = make_store();
        store.block("peer-malicioso");
        let validator =
            TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator();

        let _ = validator(&make_ctx("peer-malicioso")).await;
        assert!(!store.contains("peer-malicioso"));
    }
}
