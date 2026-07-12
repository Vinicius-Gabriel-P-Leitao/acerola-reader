use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for ChapterPage {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "page_number",
            "image_url",
            "downloaded",
            "chapter_fk",
        ]
    }
    fn table_name() -> &'static str {
        "chapter_page"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for ChapterPage {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(self.page_number)
            .bind(&self.image_url)
            .bind(self.downloaded)
            .bind(self.chapter_fk)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.page_number)
            .bind(&self.image_url)
            .bind(self.downloaded)
            .bind(self.chapter_fk)
            .bind(self.id) // WHERE id = ?
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterPage {
    pub id: i64,
    pub page_number: i64,
    pub image_url: String,
    pub downloaded: bool,
    pub chapter_fk: i64,
}
