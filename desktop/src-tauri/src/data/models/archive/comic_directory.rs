use crate::data::repositories::base::{Bindable, Entity};
use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

/// Contrato com o [`crate::data::repositories::base::Repository`] genérico.
impl Entity for ComicDirectory {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "name",
            "path",
            "cover",
            "banner",
            "last_modified",
            "chapter_template_fk",
            "external_sync_enabled",
            "hidden",
        ]
    }
    fn table_name() -> &'static str {
        "comic_directory"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ComicDirectory {
    fn bind_insert<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.name)
            .bind(&self.path)
            .bind(&self.cover)
            .bind(&self.banner)
            .bind(self.last_modified)
            .bind(self.chapter_template_fk)
            .bind(self.external_sync_enabled)
            .bind(self.hidden)
    }

    fn bind_update<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.name)
            .bind(&self.path)
            .bind(&self.cover)
            .bind(&self.banner)
            .bind(self.last_modified)
            .bind(self.chapter_template_fk)
            .bind(self.external_sync_enabled)
            .bind(self.hidden)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

/// Diretório de quadrinhos gerenciado pela aplicação.                                                                                                                                                                                                                                      ///
/// Equivalente a um `@Entity` do JPA — mapeia para a tabela `comic_directory`.
/// Migration em `src-tauri/migrations/archive`.
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ComicDirectory {
    pub id: i64,
    pub name: String,
    pub path: String,
    pub cover: Option<String>,
    pub banner: Option<String>,
    pub last_modified: i64,
    pub chapter_template_fk: Option<i64>,
    pub external_sync_enabled: bool,
    pub hidden: bool,
}
