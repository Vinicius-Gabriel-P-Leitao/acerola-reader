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

/// Serializa i64 como string no JSON para evitar perda de precisão no JavaScript.
/// Números i64 podem exceder Number.MAX_SAFE_INTEGER (2^53 - 1) do JS,
/// causando valores truncados e comparações que falham silenciosamente.
mod i64_as_string {
    use serde::{self, Deserialize, Deserializer, Serializer};

    pub fn serialize<S>(value: &i64, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(&value.to_string())
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<i64, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s = String::deserialize(deserializer)?;
        s.parse::<i64>().map_err(serde::de::Error::custom)
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ComicCategory {
    pub id: Option<i64>,
    #[serde(with = "i64_as_string")]
    pub comic_directory_fk: i64,
    pub category_id: i64,
}
