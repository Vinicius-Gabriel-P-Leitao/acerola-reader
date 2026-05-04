use sqlx::SqlitePool;

use crate::{
    data::{
        models::views::ComicSummaryView,
        repositories::views::HomeRepository,
    },
    infra::error::ComicError,
};

pub struct HomeService {
    repo: HomeRepository,
}

impl HomeService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { repo: HomeRepository::new(pool) }
    }

    pub async fn get_all(&self) -> Result<Vec<ComicSummaryView>, ComicError> {
        let comics = self.repo.base.find_all().await?;
        Ok(comics)
    }
}
