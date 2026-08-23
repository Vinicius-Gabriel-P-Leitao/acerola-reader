use std::{
    collections::HashSet,
    sync::{Arc, Mutex},
};

/// Impede que duas sessões de `acerola/sync-files/1` concorram pro mesmo peer — a mesma
/// corrida que o lado Android já observa em produção (retry do usuário, reconexão automática,
/// ou o transporte entregando duas conexões físicas pro mesmo peer, ver fix 1/3 no
/// `acerola-p2p`). Compartilhado entre `FileSyncOutbound` e `FileSyncInbound`: uma inbound e
/// uma outbound pro mesmo peer se bloqueiam entre si, não só sessões do mesmo papel.
///
/// Sync de histórico não precisa disso — é um round-trip curto de JSON, a janela de
/// sobreposição é desprezível comparada à transferência de bytes de arquivos.
#[derive(Default)]
pub struct FileSyncSessionGuard {
    active_peers: Mutex<HashSet<String>>,
}

impl FileSyncSessionGuard {
    pub fn new() -> Arc<Self> {
        Arc::new(Self::default())
    }

    /// Tenta reservar a sessão pro peer. `None` se já existe uma sessão ativa (inbound ou
    /// outbound) pro mesmo peer — quem chamou deve recusar a conexão sem abrir os streams
    /// nem tocar o serviço de sync.
    pub fn try_acquire(self: &Arc<Self>, peer_id: &str) -> Option<FileSyncSessionLease> {
        let mut active_peers = self.active_peers.lock().expect("file sync guard mutex poisoned");
        if !active_peers.insert(peer_id.to_string()) {
            return None;
        }

        Some(FileSyncSessionLease { guard: Arc::clone(self), peer_id: peer_id.to_string() })
    }
}

/// RAII: libera o peer do guard quando a sessão termina, seja por sucesso, erro, ou drop
/// antecipado — nunca precisa de um caminho de liberação manual.
pub struct FileSyncSessionLease {
    guard: Arc<FileSyncSessionGuard>,
    peer_id: String,
}

impl Drop for FileSyncSessionLease {
    fn drop(&mut self) {
        self.guard
            .active_peers
            .lock()
            .expect("file sync guard mutex poisoned")
            .remove(&self.peer_id);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_second_attempt_for_same_peer_is_rejected_while_first_is_active() {
        let guard = FileSyncSessionGuard::new();

        let first_lease = guard.try_acquire("peer-1").expect("primeira aquisição deveria suceder");
        assert!(guard.try_acquire("peer-1").is_none());

        drop(first_lease);
        assert!(guard.try_acquire("peer-1").is_some());
    }

    #[test]
    fn test_different_peers_do_not_block_each_other() {
        let guard = FileSyncSessionGuard::new();

        let _lease_a = guard.try_acquire("peer-a").expect("peer-a deveria adquirir");
        assert!(guard.try_acquire("peer-b").is_some());
    }
}
