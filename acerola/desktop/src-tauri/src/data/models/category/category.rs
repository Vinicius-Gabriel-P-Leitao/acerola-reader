use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

use crate::data::repositories::{Bindable, Entity};

impl Entity for Category {
    fn columns() -> &'static [&'static str] {
        &["id", "name", "color"]
    }
    fn table_name() -> &'static str {
        "category"
    }
    fn id(&self) -> i64 {
        self.id.unwrap_or(0)
    }
}

impl Bindable for Category {
    fn bind_insert<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(self.id).bind(&self.name).bind(self.color)
    }

    fn bind_update<'query>(
        &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query.bind(&self.name).bind(self.color).bind(self.id)
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct Category {
    pub id: Option<i64>,
    pub name: String,
    pub color: i64,
}
