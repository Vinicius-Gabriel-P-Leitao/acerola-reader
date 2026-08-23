use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

/// Contrato com o [`crate::data::repositories::Repository`] genérico.
impl Entity for ReadingHistory {
    fn columns() -> &'static [&'static str] {
        &["comic_directory_id", "chapter_archive_id", "last_page", "is_completed", "updated_at"]
    }
    fn table_name() -> &'static str {
        "reading_history"
    }
    fn id(&self) -> i64 {
        self.comic_directory_id
    }
}

/// Garante que o código consiga serializar o sql para o objeto.
impl Bindable for ReadingHistory {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.comic_directory_id)
            .bind(self.chapter_archive_id)
            .bind(self.last_page)
            .bind(self.is_completed)
            .bind(self.updated_at)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.chapter_archive_id)
            .bind(self.last_page)
            .bind(self.is_completed)
            .bind(self.updated_at)
            .bind(self.comic_directory_id) // <- id pro WHERE id = ?
    }
}

// NOTE: Migration em src-tauri\migrations\history
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ReadingHistory {
    pub comic_directory_id: i64,
    pub chapter_archive_id: i64,
    pub last_page: i64,
    pub is_completed: bool,
    pub updated_at: i64,
}
