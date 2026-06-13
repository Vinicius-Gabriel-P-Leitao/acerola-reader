use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for VolumeArchive {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "name",
            "path",
            "volume_sort",
            "is_special",
            "cover",
            "banner",
            "comic_directory_fk",
            "last_modified",
        ]
    }
    fn table_name() -> &'static str {
        "volume_archive"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

impl Bindable for VolumeArchive {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.name)
            .bind(&self.path)
            .bind(&self.volume_sort)
            .bind(self.is_special)
            .bind(&self.cover)
            .bind(&self.banner)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.name)
            .bind(&self.path)
            .bind(&self.volume_sort)
            .bind(self.is_special)
            .bind(&self.cover)
            .bind(&self.banner)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
            .bind(self.id)
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct VolumeArchive {
    pub id: i64,
    pub name: String,
    pub path: String,
    pub volume_sort: String,
    pub is_special: bool,
    pub cover: Option<String>,
    pub banner: Option<String>,
    pub comic_directory_fk: i64,
    pub last_modified: i64,
}
