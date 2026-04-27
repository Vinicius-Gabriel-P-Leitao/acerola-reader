use sqlx::SqlitePool;

use crate::data::{
    models::views::ComicSummaryView, repositories::Repository,
};

pub struct HomeRepository {
    pub base: Repository<ComicSummaryView>,
    pool: SqlitePool,
}

impl HomeRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool.clone()), pool }
    }
}
