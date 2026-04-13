use sqlx::SqlitePool;

use crate::data::{
    models::views::comic_summary_view::ComicSummaryView, repositories::base::Repository,
};

pub struct HomeRepository {
    pub base: Repository<ComicSummaryView>,
    pool: SqlitePool,
}

impl HomeRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            base: Repository::new(pool.clone()),
            pool,
        }
    }
}
