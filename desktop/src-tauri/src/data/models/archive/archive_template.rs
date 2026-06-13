use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize, sqlx::Type)]
#[sqlx(type_name = "TEXT", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum SortType {
    Chapter,
    Volume,
}

impl Entity for ArchiveTemplate {
    fn columns() -> &'static [&'static str] {
        &["id", "label", "pattern", "type", "is_default", "priority"]
    }
    fn table_name() -> &'static str {
        "archive_template"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for ArchiveTemplate {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.label)
            .bind(&self.pattern)
            .bind(&self.sort_type)
            .bind(self.is_default)
            .bind(self.priority)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.label)
            .bind(&self.pattern)
            .bind(&self.sort_type)
            .bind(self.is_default)
            .bind(self.priority)
            .bind(self.id)
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ArchiveTemplate {
    pub id: i64,
    pub label: String,
    pub pattern: String,
    #[sqlx(rename = "type")]
    pub sort_type: SortType,
    pub is_default: bool,
    pub priority: i64,
}
