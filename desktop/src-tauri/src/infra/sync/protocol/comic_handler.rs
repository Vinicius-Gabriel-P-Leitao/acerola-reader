use std::sync::Arc;

use acerola_p2p::api::{
    error::P2pError,
    peer::PeerIdentity,
    protocol::{EventEmitter, Handler},
};
use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::{
    core::services::sync::file_sync::FileSyncService,
    data::{
        models::sync::sync_history_log::SyncHistoryLogEntry,
        repositories::sync::sync_history_log_repo::SyncHistoryLogRepository,
    },
    infra::sync::{
        framing::{framed_reader, framed_writer, read_json, write_json, FramedReader, FramedWriter},
        messages::{ComicSyncRequest, FileManifest, FileWantList},
        protocol::{
            comic_sync_registry::PendingComicSyncRegistry,
            file_session_guard::FileSyncSessionGuard,
            transfer::{emit_busy, read_or_busy, receive_files, send_files, write_session_busy},
        },
    },
};

const LOG_KIND: &str = "comic";
const STARTED_EVENT: &str = "sync:comic:started";
const PROGRESS_EVENT: &str = "sync:comic:progress";
const COMPLETE_EVENT: &str = "sync:comic:complete";
const ERROR_EVENT: &str = "sync:comic:error";

/// Lado que INICIA um sync individual de UM quadrinho (`acerola/sync-comic/1`). Reaproveita o
/// mesmo mecanismo de diff/want-list/transfer de `acerola/sync-files/1`
/// (`file_handler.rs`/`transfer.rs`), só trocando manifesto de biblioteca inteira por
/// manifesto escopado a um `comic_name` — ver o comentário de design no
/// `FileSyncService::build_manifest_for_comic`. Isso resolve push e pull com o mesmo código:
/// se eu tenho o quadrinho e o peer não, o manifesto dele vem vazio e o `diff_wanted` dele pede
/// tudo de mim; se é o contrário, o meu vem vazio e sou eu que peço tudo dele.
///
/// Sequência (uma mensagem a mais que `FileSyncOutbound`, no começo, pra informar QUAL
/// quadrinho — sem isso o lado que responde não teria como escopar quando o meu manifesto
/// vier vazio, caso "pull"):
/// 1. escreve `ComicSyncRequest{comic_name}` → 2. escreve manifesto local escopado → 3. lê
///    manifesto do peer (escopado) → 4. escreve o que quer → 5. lê o que o peer quer → 6. lê
///    os arquivos que pediu → 7. escreve os arquivos que o peer pediu.
pub struct ComicSyncOutbound {
    emit: EventEmitter,
    service: FileSyncService,
    log_repo: SyncHistoryLogRepository,
    guard: Arc<FileSyncSessionGuard>,
    registry: Arc<PendingComicSyncRegistry>,
}

impl ComicSyncOutbound {
    pub fn new(
        emit: EventEmitter, service: FileSyncService, log_repo: SyncHistoryLogRepository,
        guard: Arc<FileSyncSessionGuard>, registry: Arc<PendingComicSyncRegistry>,
    ) -> Self {
        Self { emit, service, log_repo, guard, registry }
    }

    async fn run(
        &self, peer_id: &str, writer: &mut FramedWriter, reader: &mut FramedReader,
    ) -> Result<(), P2pError> {
        let comic_name = self.registry.take(peer_id).ok_or_else(|| {
            P2pError::StreamFailed("no pending comic sync scope registered for this peer".into())
        })?;

        write_json(writer, &ComicSyncRequest { comic_name: comic_name.clone() }).await?;

        let local_manifest = self.service.build_manifest_for_comic(&comic_name).await?;
        write_json(writer, &local_manifest).await?;

        let peer_manifest: FileManifest = read_or_busy(reader).await?;

        let my_wanted = self.service.diff_wanted(&peer_manifest).await?;
        write_json(writer, &FileWantList { wanted: my_wanted.clone() }).await?;

        let their_wanted: FileWantList = read_json(reader).await?;

        // Fase 1: o peer envia primeiro o que eu pedi.
        receive_files(reader, my_wanted.len(), &self.service, &self.emit, PROGRESS_EVENT, ERROR_EVENT)
            .await?;

        // Fase 2: eu envio o que o peer pediu.
        send_files(writer, &their_wanted.wanted, &self.service, &self.emit, PROGRESS_EVENT).await?;

        Ok(())
    }
}

#[async_trait]
impl Handler for ComicSyncOutbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let Some(_lease) = self.guard.try_acquire(&peer.id) else {
            write_session_busy(send).await;
            emit_busy(&self.emit, ERROR_EVENT, &peer.id);
            return Ok(());
        };

        let mut writer = framed_writer(send);
        let mut reader = framed_reader(recv);

        (self.emit)(STARTED_EVENT, peer.id.clone());

        match self.run(&peer.id, &mut writer, &mut reader).await {
            Ok(()) => {
                (self.emit)(COMPLETE_EVENT, peer.id.clone());
                self.log_repo
                    .base
                    .insert(&SyncHistoryLogEntry::new(&peer.id, LOG_KIND, "complete", None))
                    .await
                    .ok();
                Ok(())
            },
            Err(error) => {
                let message = error.to_string();
                (self.emit)(
                    ERROR_EVENT,
                    serde_json::json!({ "peerId": peer.id, "message": &message }).to_string(),
                );
                self.log_repo
                    .base
                    .insert(&SyncHistoryLogEntry::new(&peer.id, LOG_KIND, "error", Some(&message)))
                    .await
                    .ok();
                Err(error)
            },
        }
    }
}

/// Lado que RESPONDE à sessão de sync individual — mesma sequência lógica do outbound, com os
/// papéis de leitura/escrita invertidos em cada passo. O `comic_name` a escopar vem do
/// `ComicSyncRequest` recebido, não de um registro local (só o lado que inicia precisa do
/// `PendingComicSyncRegistry`).
pub struct ComicSyncInbound {
    emit: EventEmitter,
    service: FileSyncService,
    log_repo: SyncHistoryLogRepository,
    guard: Arc<FileSyncSessionGuard>,
}

impl ComicSyncInbound {
    pub fn new(
        emit: EventEmitter, service: FileSyncService, log_repo: SyncHistoryLogRepository,
        guard: Arc<FileSyncSessionGuard>,
    ) -> Self {
        Self { emit, service, log_repo, guard }
    }

    async fn run(&self, writer: &mut FramedWriter, reader: &mut FramedReader) -> Result<(), P2pError> {
        let request: ComicSyncRequest = read_or_busy(reader).await?;

        let peer_manifest: FileManifest = read_json(reader).await?;

        let local_manifest = self.service.build_manifest_for_comic(&request.comic_name).await?;
        write_json(writer, &local_manifest).await?;

        let their_wanted: FileWantList = read_json(reader).await?;

        let my_wanted = self.service.diff_wanted(&peer_manifest).await?;
        write_json(writer, &FileWantList { wanted: my_wanted.clone() }).await?;

        // Fase 1: eu envio primeiro o que o peer (outbound) pediu.
        send_files(writer, &their_wanted.wanted, &self.service, &self.emit, PROGRESS_EVENT).await?;

        // Fase 2: eu recebo o que eu pedi.
        receive_files(reader, my_wanted.len(), &self.service, &self.emit, PROGRESS_EVENT, ERROR_EVENT)
            .await?;

        Ok(())
    }
}

#[async_trait]
impl Handler for ComicSyncInbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let Some(_lease) = self.guard.try_acquire(&peer.id) else {
            write_session_busy(send).await;
            emit_busy(&self.emit, ERROR_EVENT, &peer.id);
            return Ok(());
        };

        let mut writer = framed_writer(send);
        let mut reader = framed_reader(recv);

        (self.emit)(STARTED_EVENT, peer.id.clone());

        match self.run(&mut writer, &mut reader).await {
            Ok(()) => {
                (self.emit)(COMPLETE_EVENT, peer.id.clone());
                self.log_repo
                    .base
                    .insert(&SyncHistoryLogEntry::new(&peer.id, LOG_KIND, "complete", None))
                    .await
                    .ok();
                Ok(())
            },
            Err(error) => {
                let message = error.to_string();
                (self.emit)(
                    ERROR_EVENT,
                    serde_json::json!({ "peerId": peer.id, "message": &message }).to_string(),
                );
                self.log_repo
                    .base
                    .insert(&SyncHistoryLogEntry::new(&peer.id, LOG_KIND, "error", Some(&message)))
                    .await
                    .ok();
                Err(error)
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Mutex;

    use acerola_p2p::api::peer::PeerIdentity;

    use super::*;

    type RecordedEvents = Arc<Mutex<Vec<(String, String)>>>;

    fn mock_emitter() -> (EventEmitter, RecordedEvents) {
        let events = Arc::new(Mutex::new(Vec::new()));
        let events_clone = Arc::clone(&events);
        let emit: EventEmitter =
            Arc::new(move |name, data| events_clone.lock().unwrap().push((name.to_string(), data)));
        (emit, events)
    }

    async fn setup() -> (SyncHistoryLogRepository, FileSyncService, tempfile::TempDir) {
        let pool = crate::tests::utils::setup_test_db::setup_test_db().await;
        let temp_dir = tempfile::tempdir().unwrap();
        let root = temp_dir.path().to_path_buf();
        let service = FileSyncService::new(pool.clone(), move || root.clone());
        let log_repo = SyncHistoryLogRepository::new(pool);
        (log_repo, service, temp_dir)
    }

    /// Sem `comic_name` registrado pro peer, o outbound aborta a sessão sem nunca chegar a
    /// escrever nada no stream — evita disparar o resto do protocolo com um escopo
    /// indefinido, o que aconteceria se o comando Tauri `sync_comic` esquecesse de chamar
    /// `registry.set(...)` antes de `connect()`.
    #[tokio::test]
    async fn outbound_fails_fast_when_no_comic_is_pending_for_the_peer() {
        let (log_repo, service, _dir) = setup().await;
        let guard = FileSyncSessionGuard::new();
        let registry = PendingComicSyncRegistry::new();
        let (emit, events) = mock_emitter();

        let peer = PeerIdentity { id: "peer-no-scope".to_string(), device_id: None };
        let outbound = ComicSyncOutbound::new(emit, service, log_repo, guard, registry);

        let result = outbound
            .handle(&peer, Box::new(tokio::io::empty()), Box::new(tokio::io::empty()))
            .await;

        assert!(result.is_err());
        let recorded = events.lock().unwrap();
        assert!(recorded.iter().any(|(name, _)| name == "sync:comic:error"));
    }

    /// Mesma corrida documentada em `file_handler.rs`, aplicada ao protocolo individual: uma
    /// segunda sessão pro mesmo peer enquanto uma já está ativa é rejeitada sem tocar o
    /// serviço, reaproveitando o `FileSyncSessionGuard` compartilhado com `FILE_SYNC_ALPN`.
    #[tokio::test]
    async fn inbound_rejects_second_session_for_same_peer_without_touching_the_service() {
        let (log_repo, service, _dir) = setup().await;
        let guard = FileSyncSessionGuard::new();
        let (emit, events) = mock_emitter();

        let peer = PeerIdentity { id: "peer-busy".to_string(), device_id: None };
        let _held_lease = guard.try_acquire(&peer.id).expect("primeira reserva deveria suceder");

        let inbound = ComicSyncInbound::new(emit, service, log_repo, guard);

        let result =
            inbound.handle(&peer, Box::new(tokio::io::empty()), Box::new(tokio::io::empty())).await;

        assert!(result.is_ok());
        let recorded = events.lock().unwrap();
        assert_eq!(recorded.len(), 1, "esperava só o evento de busy, recebeu: {recorded:?}");
        assert_eq!(recorded[0].0, "sync:comic:error");
        assert!(recorded[0].1.contains("already in progress"));
    }
}
