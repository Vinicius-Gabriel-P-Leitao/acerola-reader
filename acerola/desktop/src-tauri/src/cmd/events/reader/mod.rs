use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReaderChapterPayload {
    pub id: String,
    pub name: String,
    pub path: String,
    pub chapter_sort: String,
    pub volume_id: Option<String>,
    pub volume_name: Option<String>,
    pub is_special: bool,
    pub last_modified: i64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReaderSessionPayload {
    pub chapter: ReaderChapterPayload,
    pub page_count: usize,
    pub current_page: usize,
    pub cache_capacity: usize,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReaderPagePayload {
    pub chapter_id: String,
    pub index: usize,
    pub total: usize,
    pub mime_type: String,
    pub bytes: Vec<u8>,
    pub cache_hit: bool,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReaderStatusPayload {
    pub is_open: bool,
    pub chapter_id: Option<String>,
    pub page_count: usize,
    pub current_page: Option<usize>,
    pub cache_keys: Vec<usize>,
    pub cache_capacity: usize,
}
