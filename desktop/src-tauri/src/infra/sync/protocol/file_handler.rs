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
    infra::{
        error::RpcError,
        sync::{
            framing::{
                framed_reader, framed_writer, read_json, receive_file_to_disk, send_file_bytes,
                write_json, FramedReader, FramedWriter,
            },
            messages::{FileHeader, FileManifest, FileWantList, SessionBusy},
            protocol::file_session_guard::FileSyncSessionGuard,
        },
    },
};

const LOG_KIND: &str = "files";

/// Texto de rejeição usado tanto no evento local (`emit_busy`) quanto na mensagem `SessionBusy`
/// escrita no wire (`write_session_busy`) — mesmo motivo nos dois lugares.
const SESSION_BUSY_REASON: &str = "file sync session already in progress for this peer";

/// Valor do campo `error` em `SessionBusy` — mesmo literal usado pelo Android
/// (`protocol/files/exchange.rs::SESSION_BUSY_TAG`), é o que os dois lados usam pra reconhecer
/// essa mensagem específica em vez de um `FileManifest` normal.
const SESSION_BUSY_TAG: &str = "busy";

/// Emitido quando `FileSyncSessionGuard` recusa uma sessão porque já existe uma ativa pro
/// mesmo peer — reaproveita o evento `sync:files:error` já tratado pelo frontend em vez de
/// introduzir um evento novo (ver `use-network-sync.svelte.ts::parseErrorPayload`).
fn emit_busy(emit: &EventEmitter, peer_id: &str) {
    (emit)(
        "sync:files:error",
        serde_json::json!({
            "peerId": peer_id,
            "message": SESSION_BUSY_REASON,
        })
        .to_string(),
    );
}

/// Escreve a mensagem de rejeição de sessão diretamente no stream, no lugar do manifesto —
/// usado quando `FileSyncSessionGuard::try_acquire` já barrou a sessão em `handle()`, antes do
/// resto do protocolo rodar. Recebe `send` cru (não um `FramedWriter` já montado) porque é
/// exatamente o estado em que `handle()` está no momento da rejeição: o stream ainda não foi
/// envolvido em nenhum framing. Melhor esforço: se a escrita falhar (peer já caiu, stream já
/// fechado), ignora — a sessão já foi rejeitada localmente de qualquer forma.
async fn write_session_busy(send: Box<dyn AsyncWrite + Send + Unpin>) {
    let mut writer = framed_writer(send);
    let _ = write_json(
        &mut writer,
        &SessionBusy { error: SESSION_BUSY_TAG.to_string(), reason: SESSION_BUSY_REASON.to_string() },
    )
    .await;
}

/// Lê a primeira mensagem do peer podendo ser tanto o `FileManifest` normal quanto uma
/// rejeição `SessionBusy` — sem tag de enum no wire (mesma convenção do resto do protocolo),
/// a única forma de saber qual é espiar o JSON bruto antes de decidir o tipo concreto. Só os
/// dois pontos de leitura inicial em `run()` usam isso: depois da primeira mensagem, uma
/// sessão aceita nunca mais manda `SessionBusy`.
async fn read_manifest_or_busy(reader: &mut FramedReader) -> Result<FileManifest, P2pError> {
    let value: serde_json::Value = read_json(reader).await?;

    let is_busy = value.get("error").and_then(|tag| tag.as_str()) == Some(SESSION_BUSY_TAG);
    if is_busy {
        let reason = value.get("reason").and_then(|r| r.as_str()).unwrap_or("peer is busy");
        return Err(P2pError::StreamFailed(format!("peer busy: {reason}")));
    }

    serde_json::from_value(value)
        .map_err(|err| RpcError::Deserialize(err.to_string()).into())
}

/// Envia, em sequência, os arquivos de `wanted` (o que o peer pediu de nós). Se um item não
/// existir mais localmente (corrida com uma deleção concorrente), envia um header
/// "indisponível" (`size: 0`) em vez de abortar a sessão inteira.
async fn send_files(
    writer: &mut FramedWriter, wanted: &[(String, String)], service: &FileSyncService,
    emit: &EventEmitter,
) -> Result<(), P2pError> {
    for (comic_name, chapter) in wanted {
        let resolved = service.resolve_local_file(comic_name, chapter).await?;

        let Some((path, size, checksum, file_name)) = resolved else {
            write_json(
                writer,
                &FileHeader {
                    comic_name: comic_name.clone(),
                    chapter: chapter.clone(),
                    file_name: String::new(),
                    size: 0,
                    checksum: None,
                },
            )
            .await?;
            continue;
        };

        write_json(
            writer,
            &FileHeader {
                comic_name: comic_name.clone(),
                chapter: chapter.clone(),
                file_name,
                size,
                checksum,
            },
        )
        .await?;

        send_file_bytes(writer, &path, size).await?;
        (emit)("sync:files:progress", format!("{} - {}", comic_name, chapter));
    }

    Ok(())
}

/// Recebe, em sequência, `expected_count` arquivos, gravando em streaming num arquivo
/// temporário e só movendo pro destino final depois de verificar o SHA-256 contra o
/// anunciado no header. Descarta silenciosamente headers "indisponíveis" (`size: 0`) ou
/// arquivos que falharem a verificação de integridade, sem abortar a sessão.
async fn receive_files(
    reader: &mut FramedReader, expected_count: usize, service: &FileSyncService,
    emit: &EventEmitter,
) -> Result<(), P2pError> {
    let incoming_dir = service.library_root().join("synced");
    tokio::fs::create_dir_all(&incoming_dir).await.map_err(RpcError::from)?;

    for _ in 0..expected_count {
        let header: FileHeader = read_json(reader).await?;

        // TEMP DEBUG: rastreia o `chapter` exatamente como chegou pelo fio, antes de
        // qualquer gravação — prova se um rótulo com extensão (ex: "1.cbz") já vem assim
        // do peer ou se é introduzido depois, no lado que recebe.
        tracing::debug!(
            comic_name = %header.comic_name,
            chapter = %header.chapter,
            file_name = %header.file_name,
            "[FileSync] header recebido pelo fio"
        );

        if header.size == 0 {
            continue;
        }

        let temp_path = incoming_dir.join(format!(".incoming-{}.tmp", rand::random::<u64>()));
        let computed_checksum = receive_file_to_disk(reader, &temp_path, header.size).await?;

        if let Some(expected) = &header.checksum {
            if expected != &computed_checksum {
                tokio::fs::remove_file(&temp_path).await.ok();
                (emit)(
                    "sync:files:error",
                    format!("checksum mismatch: {} - {}", header.comic_name, header.chapter),
                );
                continue;
            }
        }

        service
            .persist_received_chapter(
                &header.comic_name,
                &header.chapter,
                &header.file_name,
                &temp_path,
                computed_checksum,
            )
            .await?;

        (emit)("sync:files:progress", format!("{} - {}", header.comic_name, header.chapter));
    }

    Ok(())
}

/// Lado que INICIA a sessão de sync de arquivos. Sequência (cada passo alterna quem
/// escreve/lê, nunca os dois escrevendo ao mesmo tempo no stream):
/// 1. escreve manifesto local → 2. lê manifesto do peer → 3. escreve o que quer → 4. lê o
///    que o peer quer → 5. lê os arquivos que pediu (o peer escreve primeiro nessa fase) →
///    6. escreve os arquivos que o peer pediu.
pub struct FileSyncOutbound {
    emit: EventEmitter,
    service: FileSyncService,
    log_repo: SyncHistoryLogRepository,
    guard: Arc<FileSyncSessionGuard>,
}

impl FileSyncOutbound {
    pub fn new(
        emit: EventEmitter, service: FileSyncService, log_repo: SyncHistoryLogRepository,
        guard: Arc<FileSyncSessionGuard>,
    ) -> Self {
        Self { emit, service, log_repo, guard }
    }

    async fn run(&self, writer: &mut FramedWriter, reader: &mut FramedReader) -> Result<(), P2pError> {
        let local_manifest = self.service.build_manifest().await?;
        write_json(writer, &local_manifest).await?;

        let peer_manifest = read_manifest_or_busy(reader).await?;

        let my_wanted = self.service.diff_wanted(&peer_manifest).await?;
        write_json(writer, &FileWantList { wanted: my_wanted.clone() }).await?;

        let their_wanted: FileWantList = read_json(reader).await?;

        // Fase 1: o peer envia primeiro o que eu pedi.
        receive_files(reader, my_wanted.len(), &self.service, &self.emit).await?;

        // Fase 2: eu envio o que o peer pediu.
        send_files(writer, &their_wanted.wanted, &self.service, &self.emit).await?;

        Ok(())
    }
}

#[async_trait]
impl Handler for FileSyncOutbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let Some(_lease) = self.guard.try_acquire(&peer.id) else {
            write_session_busy(send).await;
            emit_busy(&self.emit, &peer.id);
            return Ok(());
        };

        let mut writer = framed_writer(send);
        let mut reader = framed_reader(recv);

        (self.emit)("sync:files:started", peer.id.clone());

        match self.run(&mut writer, &mut reader).await {
            Ok(()) => {
                (self.emit)("sync:files:complete", peer.id.clone());
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
                    "sync:files:error",
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

/// Lado que RESPONDE à sessão de sync de arquivos — mesma sequência lógica do outbound,
/// com os papéis de leitura/escrita invertidos em cada passo.
pub struct FileSyncInbound {
    emit: EventEmitter,
    service: FileSyncService,
    log_repo: SyncHistoryLogRepository,
    guard: Arc<FileSyncSessionGuard>,
}

impl FileSyncInbound {
    pub fn new(
        emit: EventEmitter, service: FileSyncService, log_repo: SyncHistoryLogRepository,
        guard: Arc<FileSyncSessionGuard>,
    ) -> Self {
        Self { emit, service, log_repo, guard }
    }

    async fn run(&self, writer: &mut FramedWriter, reader: &mut FramedReader) -> Result<(), P2pError> {
        let peer_manifest = read_manifest_or_busy(reader).await?;

        let local_manifest = self.service.build_manifest().await?;
        write_json(writer, &local_manifest).await?;

        let their_wanted: FileWantList = read_json(reader).await?;

        let my_wanted = self.service.diff_wanted(&peer_manifest).await?;
        write_json(writer, &FileWantList { wanted: my_wanted.clone() }).await?;

        // Fase 1: eu envio primeiro o que o peer (outbound) pediu.
        send_files(writer, &their_wanted.wanted, &self.service, &self.emit).await?;

        // Fase 2: eu recebo o que eu pedi.
        receive_files(reader, my_wanted.len(), &self.service, &self.emit).await?;

        Ok(())
    }
}

#[async_trait]
impl Handler for FileSyncInbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let Some(_lease) = self.guard.try_acquire(&peer.id) else {
            write_session_busy(send).await;
            emit_busy(&self.emit, &peer.id);
            return Ok(());
        };

        let mut writer = framed_writer(send);
        let mut reader = framed_reader(recv);

        (self.emit)("sync:files:started", peer.id.clone());

        match self.run(&mut writer, &mut reader).await {
            Ok(()) => {
                (self.emit)("sync:files:complete", peer.id.clone());
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
                    "sync:files:error",
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

    /// Reproduz a corrida documentada no fix: uma segunda sessão inbound chega pro mesmo
    /// peer enquanto uma já está em andamento. Sem o guard, `handle()` chamaria
    /// `self.run()` (que abre o manifesto via `FileSyncService`) mesmo com outra sessão
    /// ativa. Como a lease já está reservada antes de `handle()` rodar, a asserção de que
    /// só o evento de "busy" foi emitido — nunca "started" — é a prova de que `run()` (e,
    /// portanto, o `FileSyncService`) nunca chegou a ser tocado.
    #[tokio::test]
    async fn inbound_rejeita_segunda_sessao_pro_mesmo_peer_sem_tocar_o_service() {
        let (log_repo, service, _dir) = setup().await;
        let guard = FileSyncSessionGuard::new();
        let (emit, events) = mock_emitter();

        let peer = PeerIdentity { id: "peer-busy".to_string(), device_id: None };
        let _held_lease = guard.try_acquire(&peer.id).expect("primeira reserva deveria suceder");

        let inbound = FileSyncInbound::new(emit, service, log_repo, guard);

        let result =
            inbound.handle(&peer, Box::new(tokio::io::empty()), Box::new(tokio::io::empty())).await;

        assert!(result.is_ok());
        let recorded = events.lock().unwrap();
        assert_eq!(recorded.len(), 1, "esperava só o evento de busy, recebeu: {recorded:?}");
        assert_eq!(recorded[0].0, "sync:files:error");
        assert!(recorded[0].1.contains("already in progress"));
    }

    /// Mesma corrida do teste acima, mas cruzando papéis: a lease foi reservada por uma
    /// sessão inbound e a segunda tentativa é outbound — prova que o guard é compartilhado
    /// entre os dois papéis, não só entre sessões do mesmo tipo.
    #[tokio::test]
    async fn outbound_e_inbound_compartilham_o_mesmo_guard_por_peer() {
        let (log_repo, service, _dir) = setup().await;
        let guard = FileSyncSessionGuard::new();
        let (emit, events) = mock_emitter();

        let peer = PeerIdentity { id: "peer-cross".to_string(), device_id: None };
        let _held_lease = guard.try_acquire(&peer.id).expect("reserva inicial deveria suceder");

        let outbound = FileSyncOutbound::new(emit, service, log_repo, guard);

        let result = outbound
            .handle(&peer, Box::new(tokio::io::empty()), Box::new(tokio::io::empty()))
            .await;

        assert!(result.is_ok());
        let recorded = events.lock().unwrap();
        assert_eq!(recorded.len(), 1);
        assert_eq!(recorded[0].0, "sync:files:error");
    }

    /// Sem nenhuma lease pré-existente, `handle()` deve seguir normalmente até `run()` —
    /// confirma que o guard não bloqueia sessões novas depois que a anterior é liberada.
    #[tokio::test]
    async fn inbound_segue_pra_run_quando_nenhuma_sessao_esta_ativa() {
        let (log_repo, service, _dir) = setup().await;
        let guard = FileSyncSessionGuard::new();
        let (emit, events) = mock_emitter();

        let peer = PeerIdentity { id: "peer-free".to_string(), device_id: None };
        let inbound = FileSyncInbound::new(emit, service, log_repo, guard);

        let _ =
            inbound.handle(&peer, Box::new(tokio::io::empty()), Box::new(tokio::io::empty())).await;

        let recorded = events.lock().unwrap();
        assert!(recorded.iter().any(|(name, _)| name == "sync:files:started"));
    }
}
