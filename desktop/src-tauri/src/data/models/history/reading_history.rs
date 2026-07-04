use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ReadingHistory {
    pub comic_directory_id: i64,
    pub chapter_archive_id: i64,
    pub last_page: i64,
    pub is_completed: bool,
    pub updated_at: i64,
}
