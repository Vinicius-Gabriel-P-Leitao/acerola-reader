use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct AuthorMetadata {
    pub id: i64,
    pub name: String,
    pub r#type: String, // Author, Artist, etc
    pub comic_metadata_fk: i64,
}

impl Entity for AuthorMetadata {
    fn columns() -> &'static [&'static str] {
        &["id", "name", "type", "comic_metadata_fk"]
    }
    fn table_name() -> &'static str {
        "author"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for AuthorMetadata {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.id).bind(&self.name).bind(&self.r#type).bind(self.comic_metadata_fk)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(&self.name).bind(&self.r#type).bind(self.comic_metadata_fk).bind(self.id)
    }
}
