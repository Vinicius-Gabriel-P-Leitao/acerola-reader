#[derive(Debug, sqlx::FromRow, Clone)]
pub struct ChapterArchiveWithComic {
    pub id: i64,
    pub chapter: String,
    pub path: String,
    pub checksum: Option<String>,
    pub last_modified: i64,
    pub comic_name: String,
}
