use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for ComicCategory {
    fn columns() -> &'static [&'static str] {
        &["id", "comic_directory_fk", "category_id"]
    }
    fn table_name() -> &'static str {
        "comic_category"
    }
    fn id(&self) -> i64 {
        self.id.unwrap_or(0)
    }
}

impl Bindable for ComicCategory {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.id).bind(self.comic_directory_fk).bind(self.category_id)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.comic_directory_fk).bind(self.category_id).bind(self.id)
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ComicCategory {
    pub id: Option<i64>,
    pub comic_directory_fk: i64,
    pub category_id: i64,
}
