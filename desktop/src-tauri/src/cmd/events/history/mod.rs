use serde::Serialize;

fn serialize_i64_as_string<S>(val: &i64, serializer: S) -> Result<S::Ok, S::Error>
where
    S: serde::Serializer,
{
    serializer.serialize_str(&val.to_string())
}

/// Payload de histórico de leitura enriquecido com dados do quadrinho e capítulo,
/// serializado para o frontend via command invoke.
#[derive(Debug, Serialize, sqlx::FromRow)]
#[serde(rename_all = "camelCase")]
pub struct ReadingHistoryPayload {
    #[serde(serialize_with = "serialize_i64_as_string")]
    pub comic_directory_id: i64,
    #[serde(serialize_with = "serialize_i64_as_string")]
    pub chapter_archive_id: i64,
    pub last_page: i64,
    pub is_completed: bool,
    pub updated_at: i64,
    pub comic_name: String,
    pub comic_cover: Option<String>,
    pub chapter_name: String,
    pub folder_name: String,
    pub chapter_path: String,
    pub chapter_sort: String,
    pub is_special: bool,
    pub last_modified: i64,
}
