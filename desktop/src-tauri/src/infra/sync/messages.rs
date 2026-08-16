use serde::{Deserialize, Serialize};

/// Uma entrada de progresso de leitura, identificada pela chave natural
/// (nome do quadrinho + rótulo do capítulo) — a única forma comparável entre
/// duas bases SQLite independentes, já que os IDs autoincrement não são portáveis.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryEntry {
    pub comic_name: String,
    pub chapter: String,
    pub last_page: i64,
    pub is_completed: bool,
    pub updated_at: i64,
}

/// Um marcador de "capítulo lido". Não tem `updated_at` — é um fato binário que só
/// existe ou não, então o merge entre devices é uma união (insert_or_ignore), não
/// last-write-wins.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReadMarker {
    pub comic_name: String,
    pub chapter: String,
    pub created_at: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct HistoryManifest {
    pub entries: Vec<HistoryEntry>,
    pub read_markers: Vec<ReadMarker>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileChapterInfo {
    pub chapter: String,
    pub file_name: String,
    pub checksum: Option<String>,
    pub size: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileComicInfo {
    pub comic_name: String,
    pub chapters: Vec<FileChapterInfo>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct FileManifest {
    pub comics: Vec<FileComicInfo>,
}

/// O que este lado quer *do outro lado* — calculado localmente depois de comparar
/// os dois manifestos, e enviado de volta pro peer decidir o que transmitir.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct FileWantList {
    pub wanted: Vec<(String, String)>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileHeader {
    pub comic_name: String,
    pub chapter: String,
    pub file_name: String,
    pub size: u64,
    pub checksum: Option<String>,
}
