use acerola_p2p::api::error::P2pError;

/// Executa uma chamada FFI síncrona (implementada em Kotlin — I/O de arquivo via SAF, query
/// Room via `runBlocking`) numa thread do pool de *blocking* do Tokio, sem travar as worker
/// threads do runtime async. Nenhuma chamada a `Arc<dyn FileSyncProvider>`/
/// `Arc<dyn HistorySyncProvider>` deve rodar direto dentro de uma `async fn` — o Kotlin por
/// trás dessas traits faz I/O bloqueante de verdade (Room, SAF/DocumentFile, hash de arquivo),
/// e travar uma worker thread do reactor com isso pode saturar o runtime inteiro sob
/// concorrência. `JoinError` (panic ou cancelamento da blocking task) vira
/// `P2pError::StreamFailed` — não há uma variante mais específica em `ConnectionError` pra
/// isso, e semanticamente essa falha invalida a sessão de stream corrente do mesmo jeito que
/// um erro de I/O de rede.
pub(crate) async fn run_blocking<F, T>(f: F) -> Result<T, P2pError>
where
    F: FnOnce() -> T + Send + 'static,
    T: Send + 'static,
{
    tokio::task::spawn_blocking(f)
        .await
        .map_err(|err| P2pError::StreamFailed(format!("blocking FFI call failed: {err}")))
}

#[cfg(test)]
mod tests {
    use std::{
        sync::{
            atomic::{AtomicU32, Ordering},
            Arc,
        },
        time::Duration,
    };

    use super::*;

    /// Com só 1 worker thread, se a chamada bloqueante rodasse direto no runtime async (sem
    /// spawn_blocking), a task de "tick" nem teria chance de progredir enquanto a chamada de
    /// 300ms está em andamento — essa é a assinatura exata do bug relatado (runtime travado
    /// durante sync-files).
    #[tokio::test(flavor = "multi_thread", worker_threads = 1)]
    async fn run_blocking_does_not_starve_concurrent_async_work() {
        let ticks = Arc::new(AtomicU32::new(0));
        let ticks_clone = Arc::clone(&ticks);

        let ticker = tokio::spawn(async move {
            for _ in 0..30 {
                tokio::time::sleep(Duration::from_millis(10)).await;
                ticks_clone.fetch_add(1, Ordering::SeqCst);
            }
        });

        let blocking = run_blocking(|| std::thread::sleep(Duration::from_millis(300)));

        let (blocking_result, _) = tokio::join!(blocking, ticker);
        assert!(blocking_result.is_ok());

        // Se a chamada bloqueante tivesse travado o único worker thread, `ticks` teria ficado
        // perto de 0 até o `sleep` de 300ms liberar o worker.
        assert!(
            ticks.load(Ordering::SeqCst) >= 15,
            "async ticker foi travado pela chamada bloqueante (starvation)"
        );
    }

    #[tokio::test]
    async fn run_blocking_propagates_join_error_as_stream_failed() {
        let result: Result<(), _> = run_blocking(|| panic!("simulated FFI panic")).await;
        assert!(matches!(result, Err(P2pError::StreamFailed(_))));
    }
}
