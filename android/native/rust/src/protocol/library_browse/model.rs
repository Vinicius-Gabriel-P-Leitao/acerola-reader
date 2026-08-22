use serde::{Deserialize, Serialize};

/// Um quadrinho do lado que respondeu, resumido a nome + contagem de capítulos — não o
/// manifesto completo (`FileManifest`/`FileChapterInfo`), que carregaria checksum e nome de
/// arquivo por capítulo à toa: o único propósito daqui é o usuário escolher QUAL quadrinho
/// pedir via `acerola/sync-comic/1` depois, não decidir o que já está sincronizado.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct ComicSummaryEntry {
    pub comic_name: String,
    pub chapter_count: u32,
    /// Reaproveita `ComicDirectory.lastModified` — sem hash novo. O peer compara contra a
    /// versão já cacheada localmente pra decidir se precisa buscar uma capa nova via
    /// `acerola/browse-cover/1`.
    pub cover_version: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub(crate) struct LibrarySummary {
    pub comics: Vec<ComicSummaryEntry>,
}
