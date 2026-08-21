use std::{collections::HashMap, sync::Arc, time::Duration};

use acerola_p2p::api::error::P2pError;
use futures::SinkExt;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use super::model::{ComicSummaryEntry, LibrarySummary};
use crate::{callbacks::FileSyncProvider, protocol::ffi_blocking::run_blocking};

const RESPONSE_READ_TIMEOUT: Duration = Duration::from_secs(15);

type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

/// Reaproveita `get_file_manifest()` (já usado por `acerola/sync-files/1`) em vez de pedir um
/// método FFI novo — só agrupa por `comic_name` e conta capítulos, descartando checksum/nome de
/// arquivo, que não servem pra esse propósito (ver `model.rs`).
pub(super) async fn build_summary(provider: &Arc<dyn FileSyncProvider>) -> Result<LibrarySummary, P2pError> {
    let provider = Arc::clone(provider);
    let entries = run_blocking(move || provider.get_file_manifest()).await?;

    let mut counts: HashMap<String, u32> = HashMap::new();
    for entry in entries {
        *counts.entry(entry.comic_name).or_insert(0) += 1;
    }

    let mut comics: Vec<ComicSummaryEntry> = counts
        .into_iter()
        .map(|(comic_name, chapter_count)| ComicSummaryEntry { comic_name, chapter_count })
        .collect();
    comics.sort_by(|a, b| a.comic_name.cmp(&b.comic_name));

    Ok(LibrarySummary { comics })
}

/// Papel inbound: o simples fato de aceitar a conexão nesta ALPN já é o pedido — não há
/// mensagem de request no wire, só a resposta. Builda o resumo local e manda de volta.
pub(super) async fn run_inbound(
    provider: &Arc<dyn FileSyncProvider>, send: Box<dyn AsyncWrite + Send + Unpin>,
) -> Result<(), P2pError> {
    let summary = build_summary(provider).await?;

    let mut writer: Writer = FramedWrite::new(send, LengthDelimitedCodec::new());
    let bytes = serde_json::to_vec(&summary)
        .map_err(|err| P2pError::StreamFailed(format!("failed to encode library summary: {err}")))?;
    writer
        .send(bytes.into())
        .await
        .map_err(|err| P2pError::StreamFailed(format!("failed to write library summary: {err}")))
}

/// Papel outbound: só lê a resposta — não escreve nada, não há fase de request.
pub(super) async fn run_outbound(recv: Box<dyn AsyncRead + Send + Unpin>) -> Result<LibrarySummary, P2pError> {
    let mut reader: Recv = FramedRead::new(recv, LengthDelimitedCodec::new());

    let frame = tokio::time::timeout(RESPONSE_READ_TIMEOUT, reader.next())
        .await
        .map_err(|_| P2pError::StreamFailed("timed out reading library summary".into()))?
        .ok_or_else(|| P2pError::StreamFailed("stream closed before library summary".into()))?
        .map_err(|err| P2pError::StreamFailed(format!("failed to read library summary: {err}")))?;

    serde_json::from_slice(&frame)
        .map_err(|err| P2pError::StreamFailed(format!("failed to decode library summary: {err}")))
}

#[cfg(test)]
mod tests {
    use std::sync::Mutex;

    use super::*;
    use crate::callbacks::FfiFileManifestEntry;

    struct StubProvider {
        entries: Vec<FfiFileManifestEntry>,
    }

    impl FileSyncProvider for StubProvider {
        fn get_file_manifest(&self) -> Vec<FfiFileManifestEntry> {
            self.entries.clone()
        }
        fn open_chapter_for_read(&self, _comic_name: String, _chapter: String) -> i64 {
            -1
        }
        fn read_chapter_chunk(&self, _handle: i64, _chunk_size: u32) -> Vec<u8> {
            vec![]
        }
        fn close_read_handle(&self, _handle: i64) {}
        fn begin_chapter_write(
            &self, _comic_name: String, _chapter: String, _file_name: String, _expected_checksum: String,
            _size_bytes: u64,
        ) -> i64 {
            -1
        }
        fn write_chapter_chunk(&self, _handle: i64, _bytes: Vec<u8>) -> bool {
            false
        }
        fn finalize_chapter_write(&self, _handle: i64) -> bool {
            false
        }
        fn abort_chapter_write(&self, _handle: i64) {}
    }

    fn entry(comic_name: &str, chapter: &str) -> FfiFileManifestEntry {
        FfiFileManifestEntry {
            comic_name: comic_name.to_string(),
            chapter: chapter.to_string(),
            file_name: format!("{chapter}.cbz"),
            checksum: "irrelevant".to_string(),
            size_bytes: 1,
        }
    }

    /// Prova o agrupamento: 3 capítulos de "Comic A" e 1 de "Comic B" viram exatamente 2
    /// entradas com as contagens corretas, ordenadas por nome.
    #[tokio::test]
    async fn build_summary_groups_by_comic_and_counts_chapters() {
        let provider: Arc<dyn FileSyncProvider> = Arc::new(StubProvider {
            entries: vec![
                entry("Comic B", "Ch. 1"),
                entry("Comic A", "Ch. 1"),
                entry("Comic A", "Ch. 2"),
                entry("Comic A", "Ch. 3"),
            ],
        });

        let summary = build_summary(&provider).await.unwrap();

        assert_eq!(summary.comics.len(), 2);
        assert_eq!(summary.comics[0].comic_name, "Comic A");
        assert_eq!(summary.comics[0].chapter_count, 3);
        assert_eq!(summary.comics[1].comic_name, "Comic B");
        assert_eq!(summary.comics[1].chapter_count, 1);
    }

    /// Round-trip real sobre um `tokio::io::duplex`: prova que `run_inbound`/`run_outbound`
    /// falam o mesmo formato de wire (sem depender de nenhum handler/ALPN, só o par de
    /// funções puras de I/O). `server_send` é o lado que `run_inbound` escreve;
    /// `client_recv` é o lado que `run_outbound` lê — mesmo par conectado pelo duplex.
    #[tokio::test]
    async fn run_outbound_reads_exactly_what_run_inbound_sent() {
        let provider: Arc<dyn FileSyncProvider> =
            Arc::new(StubProvider { entries: vec![entry("Solo Leveling", "Ch. 1"), entry("Solo Leveling", "Ch. 2")] });

        let (client_io, server_io) = tokio::io::duplex(64 * 1024);
        let (client_recv, _client_send) = tokio::io::split(client_io);
        let (_server_recv, server_send) = tokio::io::split(server_io);

        let inbound_fut = run_inbound(&provider, Box::new(server_send) as Box<dyn AsyncWrite + Send + Unpin>);
        let outbound_fut = run_outbound(Box::new(client_recv) as Box<dyn AsyncRead + Send + Unpin>);

        let (inbound_result, outbound_result) = tokio::join!(inbound_fut, outbound_fut);
        inbound_result.expect("inbound deveria completar sem erro");
        let summary = outbound_result.expect("outbound deveria receber o resumo sem erro");

        assert_eq!(summary.comics.len(), 1);
        assert_eq!(summary.comics[0].comic_name, "Solo Leveling");
        assert_eq!(summary.comics[0].chapter_count, 2);
    }
}
