/// Resultado de `ChapterRepository::get_library_summary` — só SQL, sem tocar disco (nada de
/// `tokio::fs::metadata` por capítulo), usado pelo protocolo `acerola/browse-library/1`
/// (`LibraryBrowseInbound`) pra não estourar o timeout do protocolo em bibliotecas grandes.
/// `cover_version` reaproveita `comic_directory.last_modified` — sem hash novo.
#[derive(Debug, sqlx::FromRow, Clone)]
pub struct LibrarySummaryRow {
    pub comic_name: String,
    pub chapter_count: i64,
    pub cover_version: i64,
}
