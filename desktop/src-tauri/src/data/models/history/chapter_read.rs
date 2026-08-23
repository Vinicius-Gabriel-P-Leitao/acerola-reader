use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

/// Contrato com o [`crate::data::repositories::Repository`] genérico.
impl Entity for ChapterRead {
    fn columns() -> &'static [&'static str] {
        &["comic_directory_id", "chapter_archive_id", "created_at"]
    }
    fn table_name() -> &'static str {
        "chapter_read"
    }
    fn id(&self) -> i64 {
        self.chapter_archive_id
    }
}

/// Garante que o código consiga serializar o sql para o objeto.
impl Bindable for ChapterRead {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.comic_directory_id).bind(self.chapter_archive_id).bind(self.created_at)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.created_at).bind(self.chapter_archive_id) // <- id pro WHERE id = ?
    }
}

// NOTE: Migration em src-tauri\migrations\history
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterRead {
    pub comic_directory_id: i64,
    pub chapter_archive_id: i64,
    pub created_at: i64,
}
