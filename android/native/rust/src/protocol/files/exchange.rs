use std::{collections::HashMap, sync::Arc, time::Duration};

use acerola_p2p::api::{error::P2pError, peer::PeerIdentity, protocol::EventEmitter};
use futures::SinkExt;
use serde::{de::DeserializeOwned, Serialize};
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use super::model::{build_manifest, FileComicInfo, FileHeader, FileManifest, FileSyncStats, FileWantList};
use crate::callbacks::FileSyncProvider;

const FRAME_READ_TIMEOUT: Duration = Duration::from_secs(30);
const CHUNK_SIZE: usize = 64 * 1024;
const PROGRESS_EVERY_BYTES: u64 = 1024 * 1024;

pub(super) type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
pub(super) type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

/// Escreve uma mensagem de controle como frame JSON solto (sem tag de enum) — mesmo
/// formato que o Desktop usa em `infra/sync/framing.rs::write_json`.
async fn write_json<T: Serialize>(send: &mut Writer, value: &T) -> Result<(), P2pError> {
    let bytes = serde_json::to_vec(value)
        .map_err(|err| P2pError::StreamFailed(format!("failed to encode file sync message: {err}")))?;
    send.send(bytes.into())
        .await
        .map_err(|err| P2pError::StreamFailed(format!("failed to write file sync message: {err}")))
}

async fn read_json<T: DeserializeOwned>(recv: &mut Recv) -> Result<T, P2pError> {
    let frame = tokio::time::timeout(FRAME_READ_TIMEOUT, recv.next())
        .await
        .map_err(|_| P2pError::StreamFailed("timed out reading file sync message".into()))?
        .ok_or_else(|| P2pError::StreamFailed("stream closed before file sync message".into()))?
        .map_err(|err| P2pError::StreamFailed(format!("failed to read file sync message: {err}")))?;

    serde_json::from_slice(&frame)
        .map_err(|err| P2pError::StreamFailed(format!("failed to decode file sync message: {err}")))
}

/// Escreve um frame binário cru (sem envelope JSON) — usado só para bytes de arquivo,
/// igual ao `write_bytes` do Desktop.
async fn write_bytes(send: &mut Writer, bytes: Vec<u8>) -> Result<(), P2pError> {
    send.send(bytes.into())
        .await
        .map_err(|err| P2pError::StreamFailed(format!("failed to write file chunk: {err}")))
}

async fn read_bytes(recv: &mut Recv) -> Result<Vec<u8>, P2pError> {
    let frame = tokio::time::timeout(FRAME_READ_TIMEOUT, recv.next())
        .await
        .map_err(|_| P2pError::StreamFailed("timed out reading file chunk".into()))?
        .ok_or_else(|| P2pError::StreamFailed("stream closed before file chunk".into()))?
        .map_err(|err| P2pError::StreamFailed(format!("failed to read file chunk: {err}")))?;

    Ok(frame.to_vec())
}

fn build_local_manifest(provider: &dyn FileSyncProvider) -> FileManifest {
    build_manifest(provider.get_file_manifest())
}

/// Retorna `(comic_name, chapter)` que `peer` oferece e que `local` não tem (ausente, ou
/// presente com checksum diferente — tratado como "faltando" também, sem tentar reconciliar).
fn missing_from(local: &FileManifest, peer: &FileManifest) -> Vec<(String, String)> {
    let local_checksums: HashMap<(&str, &str), Option<&str>> = local
        .comics
        .iter()
        .flat_map(|comic| {
            comic
                .chapters
                .iter()
                .map(move |chapter| ((comic.comic_name.as_str(), chapter.chapter.as_str()), chapter.checksum.as_deref()))
        })
        .collect();

    peer.comics
        .iter()
        .flat_map(|comic| {
            let local_checksums = &local_checksums;
            comic.chapters.iter().filter_map(move |chapter| {
                let key = (comic.comic_name.as_str(), chapter.chapter.as_str());
                let missing = match local_checksums.get(&key) {
                    Some(checksum) => *checksum != chapter.checksum.as_deref(),
                    None => true,
                };
                missing.then(|| (comic.comic_name.clone(), chapter.chapter.clone()))
            })
        })
        .collect()
}

fn manifest_index(manifest: &FileManifest) -> HashMap<(&str, &str), &super::model::FileChapterInfo> {
    manifest
        .comics
        .iter()
        .flat_map(|comic: &FileComicInfo| {
            comic.chapters.iter().map(move |chapter| ((comic.comic_name.as_str(), chapter.chapter.as_str()), chapter))
        })
        .collect()
}

/// Envia os capítulos pedidos, na ordem pedida. Um item que não existe mais localmente
/// (corrida com deleção concorrente) manda um `FileHeader` com `size: 0` em vez de ser
/// simplesmente pulado — o outro lado conta exatamente `requested.len()` headers, então
/// a contagem precisa ficar determinística mesmo quando um item não pode ser atendido.
async fn send_files(
    writer: &mut Writer, provider: &Arc<dyn FileSyncProvider>, requested: &[(String, String)], emit: &EventEmitter,
    peer: &PeerIdentity, stats: &mut FileSyncStats,
) -> Result<(), P2pError> {
    let local_manifest = build_local_manifest(provider.as_ref());
    let by_key = manifest_index(&local_manifest);

    for (comic_name, chapter) in requested {
        let Some(entry) = by_key.get(&(comic_name.as_str(), chapter.as_str())) else {
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

        let handle = provider.open_chapter_for_read(comic_name.clone(), chapter.clone());
        if handle < 0 {
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
        }

        write_json(
            writer,
            &FileHeader {
                comic_name: comic_name.clone(),
                chapter: chapter.clone(),
                file_name: entry.file_name.clone(),
                size: entry.size,
                checksum: entry.checksum.clone(),
            },
        )
        .await?;

        // Lê e escreve um chunk de cada vez — nunca carrega o arquivo inteiro em memória (regra 7).
        let mut sent_bytes: u64 = 0;
        let mut bytes_since_progress: u64 = 0;
        while sent_bytes < entry.size {
            let chunk = provider.read_chapter_chunk(handle, CHUNK_SIZE as u32);
            if chunk.is_empty() {
                break;
            }

            sent_bytes += chunk.len() as u64;
            bytes_since_progress += chunk.len() as u64;
            write_bytes(writer, chunk).await?;

            if bytes_since_progress >= PROGRESS_EVERY_BYTES {
                bytes_since_progress = 0;
                emit_progress(emit, peer, comic_name, chapter, sent_bytes, entry.size);
            }
        }
        provider.close_read_handle(handle);
        stats.sent_count += 1;
    }

    Ok(())
}

/// Recebe exatamente `expected_count` capítulos (a mesma contagem que este lado pediu em
/// `FileWantList`), cada um como `FileHeader` seguido de frames binários crus até somar
/// `size` bytes. `size: 0` sinaliza "peer não tem mais esse capítulo" — sem corpo a seguir.
async fn receive_files(
    reader: &mut Recv, expected_count: usize, provider: &Arc<dyn FileSyncProvider>, emit: &EventEmitter,
    peer: &PeerIdentity, stats: &mut FileSyncStats,
) -> Result<(), P2pError> {
    for _ in 0..expected_count {
        let header: FileHeader = read_json(reader).await?;

        if header.size == 0 {
            continue;
        }

        receive_one_chapter(reader, provider, emit, peer, &header, stats).await?;
    }

    Ok(())
}

async fn receive_one_chapter(
    reader: &mut Recv, provider: &Arc<dyn FileSyncProvider>, emit: &EventEmitter, peer: &PeerIdentity,
    header: &FileHeader, stats: &mut FileSyncStats,
) -> Result<(), P2pError> {
    let handle = provider.begin_chapter_write(
        header.comic_name.clone(),
        header.chapter.clone(),
        header.file_name.clone(),
        header.checksum.clone().unwrap_or_default(),
        header.size,
    );

    let mut received_bytes: u64 = 0;
    let mut bytes_since_progress: u64 = 0;
    let mut write_failed = handle < 0;

    while received_bytes < header.size {
        let chunk = read_bytes(reader).await?;
        if chunk.is_empty() {
            break;
        }

        received_bytes += chunk.len() as u64;
        bytes_since_progress += chunk.len() as u64;

        if !write_failed && !provider.write_chapter_chunk(handle, chunk) {
            write_failed = true;
        }

        if bytes_since_progress >= PROGRESS_EVERY_BYTES {
            bytes_since_progress = 0;
            emit_progress(emit, peer, &header.comic_name, &header.chapter, received_bytes, header.size);
        }
    }

    let succeeded = !write_failed && handle >= 0 && provider.finalize_chapter_write(handle);
    if succeeded {
        stats.received_count += 1;
        emit(
            "sync:files:chapter_complete",
            serde_json::json!({
                "peerId": peer.id,
                "comicName": header.comic_name,
                "chapter": header.chapter,
            })
            .to_string(),
        );
    } else {
        if handle >= 0 {
            provider.abort_chapter_write(handle);
        }
        stats.failed_count += 1;
        emit(
            "sync:files:chapter_failed",
            serde_json::json!({
                "peerId": peer.id,
                "comicName": header.comic_name,
                "chapter": header.chapter,
                "reason": "checksum or I/O failure",
            })
            .to_string(),
        );
    }

    Ok(())
}

fn emit_progress(
    emit: &EventEmitter, peer: &PeerIdentity, comic_name: &str, chapter: &str, transferred: u64, total: u64,
) {
    emit(
        "sync:files:progress",
        serde_json::json!({
            "peerId": peer.id,
            "comicName": comic_name,
            "chapter": chapter,
            "bytesTransferred": transferred,
            "totalBytes": total,
        })
        .to_string(),
    );
}

/// Executa a sessão inteira do protocolo `acerola/sync-files/1` pra um dos dois lados.
/// Schema e sequência de wire idênticos ao Desktop (`infra/sync/protocol/file_handler.rs`):
/// (1) troca de manifesto; (2) troca de want-list; (3) cada lado envia o que o outro pediu
/// e recebe o que pediu, na ordem espelhada (regra 4): outbound sempre escreve primeiro em
/// cada etapa, inbound sempre lê primeiro.
pub(super) async fn run_exchange(
    outbound_role: bool, peer: &PeerIdentity, emit: &EventEmitter, provider: &Arc<dyn FileSyncProvider>,
    send: Box<dyn AsyncWrite + Send + Unpin>, recv: Box<dyn AsyncRead + Send + Unpin>,
) -> Result<FileSyncStats, P2pError> {
    let mut writer: Writer = FramedWrite::new(send, LengthDelimitedCodec::new());
    let mut reader: Recv = FramedRead::new(recv, LengthDelimitedCodec::new());
    let mut stats = FileSyncStats::default();

    let local_manifest = build_local_manifest(provider.as_ref());

    let peer_manifest = if outbound_role {
        write_json(&mut writer, &local_manifest).await?;
        read_json(&mut reader).await?
    } else {
        let manifest = read_json(&mut reader).await?;
        write_json(&mut writer, &local_manifest).await?;
        manifest
    };

    let wanted_locally = missing_from(&local_manifest, &peer_manifest);
    let wanted_by_peer = missing_from(&peer_manifest, &local_manifest);

    emit(
        "sync:files:manifest_exchanged",
        serde_json::json!({
            "peerId": peer.id,
            "missingCount": wanted_locally.len(),
            "offeringCount": wanted_by_peer.len(),
        })
        .to_string(),
    );

    let their_wanted = if outbound_role {
        write_json(&mut writer, &FileWantList { wanted: wanted_locally.clone() }).await?;
        let their_wanted: FileWantList = read_json(&mut reader).await?;
        their_wanted.wanted
    } else {
        let their_wanted: FileWantList = read_json(&mut reader).await?;
        write_json(&mut writer, &FileWantList { wanted: wanted_locally.clone() }).await?;
        their_wanted.wanted
    };

    if outbound_role {
        // Fase 1: eu recebo o que pedi (inbound envia primeiro).
        receive_files(&mut reader, wanted_locally.len(), provider, emit, peer, &mut stats).await?;
        // Fase 2: eu envio o que o peer pediu.
        send_files(&mut writer, provider, &their_wanted, emit, peer, &mut stats).await?;
    } else {
        // Fase 1, espelhada: eu envio primeiro o que o outbound pediu.
        send_files(&mut writer, provider, &their_wanted, emit, peer, &mut stats).await?;
        // Fase 2: eu recebo o que pedi.
        receive_files(&mut reader, wanted_locally.len(), provider, emit, peer, &mut stats).await?;
    }

    Ok(stats)
}
