use std::{
    collections::HashSet,
    sync::{Arc, Mutex},
};

/// Impede duas sessões concorrentes do protocolo `acerola/sync-files/1` com o MESMO peer —
/// log real já mostrou duas conexões inbound aceitas quase simultaneamente pro mesmo peer, e
/// sem essa trava cada uma dispara sua própria varredura completa da biblioteca via
/// `run_blocking` (ver `protocol::ffi_blocking`), dobrando a carga de I/O/CPU do bug de
/// travamento do runtime que essa trava, junto com `run_blocking`, resolve. Compartilhado
/// pela MESMA instância entre `FileSyncInbound` e `FileSyncOutbound` (ver
/// `api.rs::P2PNode::new`) — a trava precisa valer pros dois papéis ao mesmo tempo, não só
/// dentro de cada um isoladamente, senão uma sessão outbound e uma inbound simultâneas pro
/// mesmo peer ainda passariam batido.
///
/// `std::sync::Mutex` (não `tokio::sync::Mutex`) é seguro aqui porque o lock nunca é mantido
/// através de um `.await` — só protege `insert`/`remove` síncronos no `HashSet`.
#[derive(Default)]
pub(crate) struct FileSyncSessionGuard {
    active_peers: Mutex<HashSet<String>>,
}

impl FileSyncSessionGuard {
    /// Tenta reservar uma sessão pra `peer_id`. `None` se já existe uma sessão ativa pra esse
    /// peer — a segunda sessão é REJEITADA, não enfileirada: enfileirar exigiria um mecanismo
    /// de fila com timeout/limite que não existe hoje em lugar nenhum do protocolo, e o peer
    /// que foi rejeitado pode simplesmente tentar de novo depois (reconexão do lado dele já é
    /// o comportamento natural do `acerola-p2p`).
    pub(crate) fn try_acquire(self: &Arc<Self>, peer_id: &str) -> Option<FileSyncSessionLease> {
        let mut active = self
            .active_peers
            .lock()
            .expect("file sync session guard poisoned");
        if !active.insert(peer_id.to_string()) {
            return None;
        }
        Some(FileSyncSessionLease {
            guard: Arc::clone(self),
            peer_id: peer_id.to_string(),
        })
    }
}

/// RAII: libera a reserva de `peer_id` automaticamente quando a sessão termina — sucesso,
/// erro (`?` saindo cedo de `run_exchange`) ou até panic. Sem isso, uma sessão que falhasse
/// no meio deixaria o peer travado pra sempre até reiniciar o app.
pub(crate) struct FileSyncSessionLease {
    guard: Arc<FileSyncSessionGuard>,
    peer_id: String,
}

impl Drop for FileSyncSessionLease {
    fn drop(&mut self) {
        self.guard
            .active_peers
            .lock()
            .expect("file sync session guard poisoned")
            .remove(&self.peer_id);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn second_acquire_for_same_peer_is_rejected_while_first_is_held() {
        let guard = Arc::new(FileSyncSessionGuard::default());
        let lease1 = guard.try_acquire("peer-a");
        assert!(lease1.is_some());
        assert!(guard.try_acquire("peer-a").is_none());
    }

    #[test]
    fn dropping_the_lease_releases_the_peer() {
        let guard = Arc::new(FileSyncSessionGuard::default());
        let lease1 = guard.try_acquire("peer-a").unwrap();
        drop(lease1);
        assert!(guard.try_acquire("peer-a").is_some());
    }

    #[test]
    fn different_peers_can_hold_sessions_simultaneously() {
        let guard = Arc::new(FileSyncSessionGuard::default());
        let _lease_a = guard.try_acquire("peer-a").unwrap();
        assert!(guard.try_acquire("peer-b").is_some());
    }
}
