use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for Banner {
    fn columns() -> &'static [&'static str] {
        &["id", "file_name", "url", "comic_metadata_fk"]
    }
    fn table_name() -> &'static str {
        "banner"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for Banner {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.id).bind(&self.file_name).bind(&self.url).bind(self.comic_metadata_fk)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(&self.file_name).bind(&self.url).bind(self.comic_metadata_fk).bind(self.id)
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct Banner {
    pub id: i64,
    pub file_name: String,
    pub url: String,
    pub comic_metadata_fk: i64,
}
