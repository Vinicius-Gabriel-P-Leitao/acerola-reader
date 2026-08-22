use std::sync::Arc;

use acerola_p2p::api::{error::P2pError, peer::PeerIdentity, protocol::EventEmitter};
use async_trait::async_trait;
use serde::de::DeserializeOwned;
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite};

use crate::{
    core::services::sync::file_sync::FileSyncService,
    infra::{
        error::RpcError,
        sync::{
            blob_context::BlobContext,
            framing::{framed_writer, read_json, write_json, FramedReader, FramedWriter},
            messages::{FileHeader, SessionBusy},
        },
    },
};

/// Publica/busca os bytes de um capítulo via `P2pBlobStore` (BLAKE3, dedup e verificação
/// automáticos), independente do stream de controle (`FramedReader`/`FramedWriter`) que só
/// carrega o `FileHeader` com o hash. Trait (em vez de `Arc<BlobContext>` direto) só pra manter
/// os testes de `send_files`/`receive_files` sem depender de um `Endpoint` iroh de verdade —
/// `BlobChapterTransfer` é a única implementação usada em produção.
#[async_trait]
pub trait ChapterTransfer: Send + Sync {
    async fn publish(&self, bytes: Vec<u8>) -> Result<String, P2pError>;

    async fn fetch_reader(
        &self, blob_hash: &str, peer: &PeerIdentity,
    ) -> Result<Box<dyn AsyncRead + Send + Unpin>, P2pError>;
}

pub struct BlobChapterTransfer {
    context: Arc<BlobContext>,
}

impl BlobChapterTransfer {
    pub fn new(context: Arc<BlobContext>) -> Self {
        Self { context }
    }
}

#[async_trait]
impl ChapterTransfer for BlobChapterTransfer {
    async fn publish(&self, bytes: Vec<u8>) -> Result<String, P2pError> {
        let store = self.context.blob_store().await?;
        let hash = store.put(bytes).await.map_err(|err| P2pError::StreamFailed(err.to_string()))?;
        Ok(hash.to_string())
    }

    async fn fetch_reader(
        &self, blob_hash: &str, peer: &PeerIdentity,
    ) -> Result<Box<dyn AsyncRead + Send + Unpin>, P2pError> {
        let store = self.context.blob_store().await?;
        let hash = blob_hash
            .parse()
            .map_err(|_| P2pError::StreamFailed(format!("invalid blob hash: {blob_hash}")))?;
        let addr = self.context.resolve_addr(peer).await?;

        store.fetch(&hash, &addr).await.map_err(|err| P2pError::StreamFailed(err.to_string()))?;
        store.get(&hash).await.map_err(|err| P2pError::StreamFailed(err.to_string()))
    }
}

/// Texto de rejeição usado tanto no evento local (`emit_busy`) quanto na mensagem `SessionBusy`
/// escrita no wire (`write_session_busy`) — mesmo motivo nos dois lugares. Compartilhado entre
/// `acerola/sync-files/1` e `acerola/sync-comic/1`: os dois competem pelo mesmo
/// `FileSyncSessionGuard` por peer (ver `file_session_guard.rs`).
pub const SESSION_BUSY_REASON: &str = "file sync session already in progress for this peer";

/// Valor do campo `error` em `SessionBusy` — mesmo literal usado pelo Android
/// (`protocol/files/exchange.rs::SESSION_BUSY_TAG`), é o que os dois lados usam pra reconhecer
/// essa mensagem específica em vez de um `FileManifest` normal.
const SESSION_BUSY_TAG: &str = "busy";

/// Emitido quando `FileSyncSessionGuard` recusa uma sessão porque já existe uma ativa pro
/// mesmo peer — reaproveita o evento `sync:files:error` já tratado pelo frontend em vez de
/// introduzir um evento novo (ver `use-network-sync.svelte.ts::parseErrorPayload`). O chamador
/// passa o nome do evento (`sync:files:error` ou `sync:comic:error`) porque cada protocolo tem
/// o seu próprio canal.
pub fn emit_busy(emit: &EventEmitter, event_name: &str, peer_id: &str) {
    (emit)(
        event_name,
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
pub async fn write_session_busy(send: Box<dyn AsyncWrite + Send + Unpin>) {
    let mut writer = framed_writer(send);
    let _ = write_json(
        &mut writer,
        &SessionBusy { error: SESSION_BUSY_TAG.to_string(), reason: SESSION_BUSY_REASON.to_string() },
    )
    .await;
}

/// Lê a primeira mensagem do peer, que pode ser tanto a mensagem esperada (`T`: `FileManifest`
/// inteiro ou escopado a um único quadrinho — ver `FileSyncService::build_manifest_for_comic`
/// — ou `ComicSyncRequest` no protocolo `acerola/sync-comic/1`) quanto uma rejeição
/// `SessionBusy` — sem tag de enum no wire (mesma convenção do resto do protocolo), a única
/// forma de saber qual é espiar o JSON bruto antes de decidir o tipo concreto. Só o(s) ponto(s)
/// de leitura inicial em cada `run()` usa(m) isso: depois da primeira mensagem, uma sessão
/// aceita nunca mais manda `SessionBusy`.
pub async fn read_or_busy<T: DeserializeOwned>(reader: &mut FramedReader) -> Result<T, P2pError> {
    let value: serde_json::Value = read_json(reader).await?;

    let is_busy = value.get("error").and_then(|tag| tag.as_str()) == Some(SESSION_BUSY_TAG);
    if is_busy {
        let reason = value.get("reason").and_then(|r| r.as_str()).unwrap_or("peer is busy");
        return Err(P2pError::StreamFailed(format!("peer busy: {reason}")));
    }

    serde_json::from_value(value).map_err(|err| RpcError::Deserialize(err.to_string()).into())
}

/// Envia, em sequência, os arquivos de `wanted` (o que o peer pediu de nós). Se um item não
/// existir mais localmente (corrida com uma deleção concorrente), envia um header
/// "indisponível" (`size: 0`) em vez de abortar a sessão inteira. Os bytes não trafegam mais
/// no stream de controle — são publicados no blob store local (`ChapterTransfer::publish`) e o
/// hash resultante vai no `FileHeader`; quem recebe busca sob demanda via `fetch_reader`.
pub async fn send_files(
    writer: &mut FramedWriter, wanted: &[(String, String)], service: &FileSyncService,
    emit: &EventEmitter, progress_event: &str, transfer: &Arc<dyn ChapterTransfer>,
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
                    blob_hash: None,
                },
            )
            .await?;
            continue;
        };

        let bytes = tokio::fs::read(&path).await.map_err(RpcError::from)?;
        let blob_hash = transfer.publish(bytes).await?;

        write_json(
            writer,
            &FileHeader { comic_name: comic_name.clone(), chapter: chapter.clone(), file_name, size, checksum, blob_hash: Some(blob_hash) },
        )
        .await?;

        (emit)(progress_event, format!("{} - {}", comic_name, chapter));
    }

    Ok(())
}

/// Recebe, em sequência, `expected_count` arquivos: cada `FileHeader` traz um `blob_hash` em
/// vez de bytes crus no stream — busca via `ChapterTransfer::fetch_reader` (que já verifica
/// integridade via BLAKE3 antes de devolver o leitor), grava num arquivo temporário e move pro
/// destino final depois de conferir o SHA-256 legado contra o anunciado no header (continuidade
/// do histórico/telemetria existente, redundante com a verificação do blob store mas barato o
/// bastante pra manter). Descarta silenciosamente headers "indisponíveis" (`size: 0`), falhas de
/// busca do blob ou mismatches de checksum, sem abortar a sessão inteira.
pub async fn receive_files(
    reader: &mut FramedReader, expected_count: usize, service: &FileSyncService, emit: &EventEmitter,
    progress_event: &str, error_event: &str, peer: &PeerIdentity, transfer: &Arc<dyn ChapterTransfer>,
) -> Result<(), P2pError> {
    let incoming_dir = service.library_root().join("synced");
    tokio::fs::create_dir_all(&incoming_dir).await.map_err(RpcError::from)?;

    for _ in 0..expected_count {
        let header: FileHeader = read_json(reader).await?;

        tracing::debug!(
            comic_name = %header.comic_name,
            chapter = %header.chapter,
            file_name = %header.file_name,
            "[FileSync] header recebido pelo fio"
        );

        if header.size == 0 {
            continue;
        }

        let Some(blob_hash) = header.blob_hash.as_deref() else {
            (emit)(error_event, format!("missing blob hash: {} - {}", header.comic_name, header.chapter));
            continue;
        };

        let mut blob_reader = match transfer.fetch_reader(blob_hash, peer).await {
            Ok(reader) => reader,
            Err(err) => {
                (emit)(
                    error_event,
                    format!("blob fetch failed: {} - {}: {err}", header.comic_name, header.chapter),
                );
                continue;
            },
        };

        let mut bytes = Vec::new();
        blob_reader.read_to_end(&mut bytes).await.map_err(|err| P2pError::StreamFailed(err.to_string()))?;

        let computed_checksum = {
            use sha2::{Digest, Sha256};
            format!("{:x}", Sha256::digest(&bytes))
        };

        if let Some(expected) = &header.checksum {
            if expected != &computed_checksum {
                (emit)(
                    error_event,
                    format!("checksum mismatch: {} - {}", header.comic_name, header.chapter),
                );
                continue;
            }
        }

        let temp_path = incoming_dir.join(format!(".incoming-{}.tmp", rand::random::<u64>()));
        tokio::fs::write(&temp_path, &bytes).await.map_err(RpcError::from)?;

        service
            .persist_received_chapter(
                &header.comic_name,
                &header.chapter,
                &header.file_name,
                &temp_path,
                computed_checksum,
            )
            .await?;

        (emit)(progress_event, format!("{} - {}", header.comic_name, header.chapter));
    }

    Ok(())
}

/// Duplo em memória de `ChapterTransfer` pra testes que não têm um `Endpoint` iroh de verdade
/// (`BlobChapterTransfer`, usado em produção, precisa disso). `shared_pair()` devolve dois
/// handles apontando pro mesmo `HashMap` interno — simula os dois lados publicando/buscando no
/// mesmo "store de rede" content-addressed.
#[cfg(test)]
pub struct InMemoryChapterTransfer {
    blobs: std::sync::Mutex<std::collections::HashMap<String, Vec<u8>>>,
}

#[cfg(test)]
impl InMemoryChapterTransfer {
    pub fn shared_pair() -> (Arc<dyn ChapterTransfer>, Arc<dyn ChapterTransfer>) {
        let shared =
            Arc::new(InMemoryChapterTransfer { blobs: std::sync::Mutex::new(std::collections::HashMap::new()) });
        (Arc::clone(&shared) as Arc<dyn ChapterTransfer>, shared as Arc<dyn ChapterTransfer>)
    }
}

#[cfg(test)]
#[async_trait]
impl ChapterTransfer for InMemoryChapterTransfer {
    async fn publish(&self, bytes: Vec<u8>) -> Result<String, P2pError> {
        use sha2::{Digest, Sha256};
        let hash = format!("{:x}", Sha256::digest(&bytes));
        self.blobs.lock().unwrap().insert(hash.clone(), bytes);
        Ok(hash)
    }

    async fn fetch_reader(
        &self, blob_hash: &str, _peer: &PeerIdentity,
    ) -> Result<Box<dyn AsyncRead + Send + Unpin>, P2pError> {
        let bytes = self
            .blobs
            .lock()
            .unwrap()
            .get(blob_hash)
            .cloned()
            .ok_or_else(|| P2pError::StreamFailed(format!("unknown blob hash in test transfer: {blob_hash}")))?;

        // `tokio::io::duplex` já implementa `AsyncRead`/`AsyncWrite` — mais simples que
        // implementar um `poll_read` manual só pra teste. Escreve tudo e fecha (EOF) antes de
        // devolver a ponta de leitura.
        use tokio::io::AsyncWriteExt;
        let (mut writer, reader) = tokio::io::duplex(bytes.len().max(1));
        tokio::spawn(async move {
            let _ = writer.write_all(&bytes).await;
            let _ = writer.shutdown().await;
        });
        Ok(Box::new(reader))
    }
}
