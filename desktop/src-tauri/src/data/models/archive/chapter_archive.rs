use sqlx::{ Sqlite, sqlite::SqliteArguments, query::Query };
use crate::data::repositories::base::{ Entity, Bindable };
use serde::{ Deserialize, Serialize };

/// Contrato com o [`crate::data::repositories::base::Repository`] genérico.
impl Entity for ChapterArchive {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "chapter",
            "path",
            "chapter_sort",
            "fast_hash",
            "comic_directory_fk",
            "last_modified",
        ]
    }
    fn table_name() -> &'static str {
        "chapter_archive"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ChapterArchive {
    fn bind_insert<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.chapter)
            .bind(&self.path)
            .bind(&self.chapter_sort)
            .bind(&self.fast_hash)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
    }

    fn bind_update<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.chapter)
            .bind(&self.path)
            .bind(&self.chapter_sort)
            .bind(&self.fast_hash)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterArchive {
    pub id: i64,
    pub chapter: String,
    pub path: String,
    pub chapter_sort: String,
    pub fast_hash: Option<String>,
    pub comic_directory_fk: i64,
    pub last_modified: i64,
}
