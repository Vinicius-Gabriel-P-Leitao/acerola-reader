use std::{
    collections::{HashMap, HashSet},
    sync::Arc,
    time::Duration,
};

use acerola_p2p::api::{error::P2pError, peer::PeerIdentity, protocol::EventEmitter};
use futures::SinkExt;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use super::model::{HistoryManifest, HistorySyncStats};
use crate::callbacks::HistorySyncProvider;

const MANIFEST_READ_TIMEOUT: Duration = Duration::from_secs(15);

pub(super) type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
pub(super) type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

pub(super) async fn write_manifest(send: &mut Writer, manifest: &HistoryManifest) -> Result<(), P2pError> {
    let bytes = serde_json::to_vec(manifest)
        .map_err(|err| P2pError::StreamFailed(format!("failed to encode history manifest: {err}")))?;
    send.send(bytes.into())
        .await
        .map_err(|err| P2pError::StreamFailed(format!("failed to write history manifest: {err}")))
}

pub(super) async fn read_manifest(recv: &mut Recv) -> Result<HistoryManifest, P2pError> {
    let frame = tokio::time::timeout(MANIFEST_READ_TIMEOUT, recv.next())
        .await
        .map_err(|_| P2pError::StreamFailed("timed out reading history manifest".into()))?
        .ok_or_else(|| P2pError::StreamFailed("stream closed before history manifest".into()))?
        .map_err(|err| P2pError::StreamFailed(format!("failed to read history manifest: {err}")))?;

    serde_json::from_slice(&frame)
        .map_err(|err| P2pError::StreamFailed(format!("failed to decode history manifest: {err}")))
}

/// Monta o manifesto local a partir do `provider` (Room, via Kotlin).
pub(super) fn build_local_manifest(provider: &dyn HistorySyncProvider) -> HistoryManifest {
    HistoryManifest {
        reading_progress: provider.get_reading_progress(),
        chapters_read: provider.get_chapters_read(),
    }
}

/// Calcula o diff do manifesto do peer contra o manifesto local e aplica via `provider`:
/// last-write-wins (por `updated_at`) pra progresso de leitura, união idempotente pra
/// capítulos lidos. Cada lado roda essa função com seu próprio manifesto local — não há
/// mais nenhuma troca de rede depois disso.
pub(super) fn diff_and_apply(
    local: &HistoryManifest, peer: HistoryManifest, provider: &Arc<dyn HistorySyncProvider>,
) -> HistorySyncStats {
    let mut stats = HistorySyncStats::default();

    let local_progress_by_key: HashMap<(&str, &str), &crate::callbacks::FfiReadingProgressEntry> = local
        .reading_progress
        .iter()
        .map(|entry| ((entry.comic_name.as_str(), entry.chapter_sort.as_str()), entry))
        .collect();

    for peer_entry in peer.reading_progress {
        let key = (peer_entry.comic_name.as_str(), peer_entry.chapter_sort.as_str());
        let should_apply = match local_progress_by_key.get(&key) {
            Some(local_entry) => peer_entry.updated_at > local_entry.updated_at,
            None => true,
        };

        if !should_apply {
            continue;
        }

        if provider.apply_reading_progress(peer_entry) {
            stats.progress_applied += 1;
        } else {
            stats.progress_skipped += 1;
        }
    }

    let local_chapters_read: HashSet<(&str, &str)> = local
        .chapters_read
        .iter()
        .map(|entry| (entry.comic_name.as_str(), entry.chapter_sort.as_str()))
        .collect();

    for peer_entry in peer.chapters_read {
        let key = (peer_entry.comic_name.as_str(), peer_entry.chapter_sort.as_str());
        if local_chapters_read.contains(&key) {
            continue;
        }

        if provider.apply_chapter_read(peer_entry) {
            stats.chapters_read_applied += 1;
        } else {
            stats.chapters_read_skipped += 1;
        }
    }

    stats
}

/// Executa a troca de manifesto de um lado da conexão e aplica o diff.
///
/// `outbound_role` decide a ordem de leitura/escrita (regra 4): o lado outbound sempre
/// escreve primeiro em cada etapa, o inbound sempre lê primeiro — nunca "quem chegar primeiro
/// escreve". Compartilhada pelos dois handlers já que a única diferença entre eles é essa ordem.
pub(super) async fn run_exchange(
    outbound_role: bool, peer: &PeerIdentity, emit: &EventEmitter, provider: &Arc<dyn HistorySyncProvider>,
    send: Box<dyn AsyncWrite + Send + Unpin>, recv: Box<dyn AsyncRead + Send + Unpin>,
) -> Result<(), P2pError> {
    let mut writer: Writer = FramedWrite::new(send, LengthDelimitedCodec::new());
    let mut reader: Recv = FramedRead::new(recv, LengthDelimitedCodec::new());

    emit("sync:history:started", started_payload(peer));

    let local_manifest = build_local_manifest(provider.as_ref());

    let peer_manifest = if outbound_role {
        write_manifest(&mut writer, &local_manifest).await?;
        read_manifest(&mut reader).await?
    } else {
        let peer_manifest = read_manifest(&mut reader).await?;
        write_manifest(&mut writer, &local_manifest).await?;
        peer_manifest
    };

    let stats = diff_and_apply(&local_manifest, peer_manifest, provider);
    emit("sync:history:complete", complete_payload(peer, &stats));

    Ok(())
}

fn started_payload(peer: &PeerIdentity) -> String {
    serde_json::json!({ "peerId": peer.id }).to_string()
}

fn complete_payload(peer: &PeerIdentity, stats: &HistorySyncStats) -> String {
    serde_json::json!({
        "peerId": peer.id,
        "progressApplied": stats.progress_applied,
        "progressSkipped": stats.progress_skipped,
        "chaptersReadApplied": stats.chapters_read_applied,
        "chaptersReadSkipped": stats.chapters_read_skipped,
    })
    .to_string()
}
