use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for ChapterMetadata {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "title",
            "chapter",
            "page_count",
            "scanlation",
            "comic_metadata_fk",
        ]
    }
    fn table_name() -> &'static str {
        "chapter_metadata"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for ChapterMetadata {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.title)
            .bind(&self.chapter)
            .bind(self.page_count)
            .bind(&self.scanlation)
            .bind(self.comic_metadata_fk)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.title)
            .bind(&self.chapter)
            .bind(self.page_count)
            .bind(&self.scanlation)
            .bind(self.comic_metadata_fk)
            .bind(self.id) // WHERE id = ?
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterMetadata {
    pub id: i64,
    pub title: Option<String>,
    pub chapter: String,
    pub page_count: Option<i64>,
    pub scanlation: Option<String>,
    pub comic_metadata_fk: i64,
}
