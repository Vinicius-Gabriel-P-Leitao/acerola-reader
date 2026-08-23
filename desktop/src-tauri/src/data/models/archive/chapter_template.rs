use crate::data::repositories::{Bindable, Entity};
use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

/// Contrato com o [`crate::data::repositories::Repository`] genérico.
impl Entity for ChapterTemplate {
    fn columns() -> &'static [&'static str] {
        &["id", "label", "pattern", "is_default", "priority"]
    }
    fn table_name() -> &'static str {
        "chapter_template"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ChapterTemplate {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.label)
            .bind(&self.pattern)
            .bind(self.is_default)
            .bind(self.priority)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.label)
            .bind(&self.pattern)
            .bind(self.is_default)
            .bind(self.priority)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterTemplate {
    pub id: i64,
    pub label: String,
    pub pattern: String,
    pub is_default: bool,
    pub priority: i64,
}
