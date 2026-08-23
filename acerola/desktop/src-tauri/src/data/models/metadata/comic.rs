use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for ComicMetadata {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "title",
            "description",
            "status",
            "publication",
            "sync_source",
            "has_comic_info",
            "comic_directory_fk",
        ]
    }
    fn table_name() -> &'static str {
        "comic_metadata"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for ComicMetadata {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.title)
            .bind(&self.description)
            .bind(&self.status)
            .bind(self.publication)
            .bind(&self.sync_source)
            .bind(self.has_comic_info)
            .bind(self.comic_directory_fk)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.title)
            .bind(&self.description)
            .bind(&self.status)
            .bind(self.publication)
            .bind(&self.sync_source)
            .bind(self.has_comic_info)
            .bind(self.comic_directory_fk)
            .bind(self.id) // WHERE id = ?
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ComicMetadata {
    pub id: i64,
    pub title: String,
    pub description: String,
    pub status: String,
    pub publication: Option<i64>,
    pub sync_source: Option<String>,
    pub has_comic_info: bool,
    pub comic_directory_fk: Option<i64>,
}
