use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterRead {
    pub comic_directory_id: i64,
    pub chapter_archive_id: i64,
    pub created_at: i64,
}
