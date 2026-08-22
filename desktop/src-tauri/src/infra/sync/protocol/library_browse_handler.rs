use acerola_p2p::api::{
    error::P2pError,
    peer::PeerIdentity,
    protocol::{EventEmitter, Handler},
};
use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::{
    core::services::sync::file_sync::FileSyncService,
    infra::sync::{
        framing::{framed_reader, framed_writer, read_json, write_json, FramedReader, FramedWriter},
        messages::LibrarySummary,
    },
};

/// Round-trip único (sem `FileSyncSessionGuard`: só lista títulos, não transfere nada, mesmo
/// perfil de custo do `history_handler.rs`) pra descobrir o que existe na biblioteca remota
/// antes de decidir sincronizar um quadrinho específico (`acerola/sync-comic/1`). Não persiste
/// em `sync_history_log` — é uma consulta de navegação, não um sync de conteúdo.
///
/// Sem mensagem de request no wire: o simples fato de conectar nesta ALPN já é o pedido, o
/// outro lado responde assim que aceita — mesmo design do Android
/// (`protocol/library_browse/exchange.rs::run_inbound`/`run_outbound`), que não lê nem escreve
/// nada além da resposta em si.
pub struct LibraryBrowseOutbound {
    emit: EventEmitter,
}

impl LibraryBrowseOutbound {
    pub fn new(emit: EventEmitter) -> Self {
        Self { emit }
    }

    async fn run(&self, reader: &mut FramedReader) -> Result<LibrarySummary, P2pError> {
        let response: LibrarySummary = read_json(reader).await?;
        Ok(response)
    }
}

#[async_trait]
impl Handler for LibraryBrowseOutbound {
    async fn handle(
        &self, peer: &PeerIdentity, _send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let mut reader = framed_reader(recv);

        match self.run(&mut reader).await {
            Ok(response) => {
                // `ComicSummaryEntry` (mensagem de wire, `snake_case`, compartilhada com o
                // Android) não é o mesmo formato que o payload de evento pro frontend
                // (`camelCase`, mesma convenção de `ConnectedPeerPayload`/`PairedPeerPayload` em
                // `cmd/events/network`) — monta o JSON do evento à mão em vez de serializar a
                // struct de wire direto, mesmo padrão já usado pelos eventos `sync:*:error`
                // deste módulo (`serde_json::json!` ad-hoc, sem struct compartilhada entre
                // `infra` e `cmd`).
                let comics: Vec<serde_json::Value> = response
                    .comics
                    .iter()
                    .map(|comic| {
                        serde_json::json!({
                            "comicName": comic.comic_name,
                            "chapterCount": comic.chapter_count,
                            "coverVersion": comic.cover_version,
                        })
                    })
                    .collect();

                (self.emit)(
                    "library:query:result",
                    serde_json::json!({ "peerId": peer.id, "comics": comics }).to_string(),
                );
                Ok(())
            },
            Err(error) => {
                let message = error.to_string();
                (self.emit)(
                    "library:query:error",
                    serde_json::json!({ "peerId": peer.id, "message": &message }).to_string(),
                );
                Err(error)
            },
        }
    }
}

/// Lado que RESPONDE: monta o resumo da biblioteca local a partir do mesmo
/// `FileSyncService::build_manifest()` usado pelo sync de arquivos (sem duplicar a lógica de
/// listar quadrinhos/capítulos) e escreve assim que a conexão é aceita — não espera nenhuma
/// mensagem do outro lado primeiro (ver comentário em `LibraryBrowseOutbound`).
pub struct LibraryBrowseInbound {
    service: FileSyncService,
}

impl LibraryBrowseInbound {
    pub fn new(service: FileSyncService) -> Self {
        Self { service }
    }

    async fn run(&self, writer: &mut FramedWriter) -> Result<(), P2pError> {
        // SQL puro (`get_library_summary`), sem tocar disco — `build_manifest()` faz um
        // `tokio::fs::metadata()` por capítulo só pra montar `size`, que essa resposta nem usa,
        // e isso estourava o timeout do protocolo em bibliotecas grandes/discos lentos.
        let comics = self.service.get_library_summary().await?;
        write_json(writer, &LibrarySummary { comics }).await?;
        Ok(())
    }
}

#[async_trait]
impl Handler for LibraryBrowseInbound {
    async fn handle(
        &self, _peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        _recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let mut writer = framed_writer(send);
        self.run(&mut writer).await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    async fn setup_service() -> (FileSyncService, tempfile::TempDir) {
        let pool = crate::tests::utils::setup_test_db::setup_test_db_with_comic().await;
        sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, checksum, comic_directory_fk, last_modified) VALUES (1, 'Cap 1', 'p/Cap 1.cbz', '1', 0, 'abc', 1, 0)")
            .execute(&pool)
            .await
            .unwrap();

        let temp_dir = tempfile::tempdir().unwrap();
        let root = temp_dir.path().to_path_buf();
        let service = FileSyncService::new(pool, move || root.clone());
        (service, temp_dir)
    }

    /// Round-trip real sobre um `tokio::io::duplex`: prova que `LibraryBrowseInbound` escreve e
    /// `LibraryBrowseOutbound` lê o mesmo formato, sem nenhuma mensagem de request no meio —
    /// mesmo contrato do Android (`run_outbound_reads_exactly_what_run_inbound_sent`).
    #[tokio::test]
    async fn outbound_reads_exactly_what_inbound_sent() {
        let (service, _dir) = setup_service().await;
        let inbound = LibraryBrowseInbound::new(service);

        let (client_io, server_io) = tokio::io::duplex(64 * 1024);
        let (client_recv, _client_send) = tokio::io::split(client_io);
        let (_server_recv, server_send) = tokio::io::split(server_io);

        let mut writer = framed_writer(Box::new(server_send) as Box<dyn AsyncWrite + Send + Unpin>);
        inbound.run(&mut writer).await.unwrap();

        let mut reader = framed_reader(Box::new(client_recv) as Box<dyn AsyncRead + Send + Unpin>);
        let outbound = LibraryBrowseOutbound::new(std::sync::Arc::new(|_, _| {}));
        let response = outbound.run(&mut reader).await.unwrap();

        assert_eq!(response.comics.len(), 1);
        assert_eq!(response.comics[0].comic_name, "Test");
        assert_eq!(response.comics[0].chapter_count, 1);
    }
}
