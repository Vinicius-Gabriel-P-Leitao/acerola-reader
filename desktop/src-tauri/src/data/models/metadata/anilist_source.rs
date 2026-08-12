use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

/// Dados brutos vindos do AniList para um quadrinho sincronizado (tabela `anilist_source`).
/// `average_score` é a nota crua do AniList (escala 0-100) — a UI converte para /10.
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct AnilistSource {
    pub id: i64,
    pub anilist_id: i64,
    pub average_score: Option<i64>,
    pub popularity: Option<i64>,
    pub trending: Option<i64>,
    pub cover_image: Option<String>,
    pub banner_image: Option<String>,
    pub comic_metadata_fk: i64,
}

impl Entity for AnilistSource {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "anilist_id",
            "average_score",
            "popularity",
            "trending",
            "cover_image",
            "banner_image",
            "comic_metadata_fk",
        ]
    }
    fn table_name() -> &'static str {
        "anilist_source"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for AnilistSource {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(self.anilist_id)
            .bind(self.average_score)
            .bind(self.popularity)
            .bind(self.trending)
            .bind(&self.cover_image)
            .bind(&self.banner_image)
            .bind(self.comic_metadata_fk)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.anilist_id)
            .bind(self.average_score)
            .bind(self.popularity)
            .bind(self.trending)
            .bind(&self.cover_image)
            .bind(&self.banner_image)
            .bind(self.comic_metadata_fk)
            .bind(self.id) // WHERE id = ?
    }
}
