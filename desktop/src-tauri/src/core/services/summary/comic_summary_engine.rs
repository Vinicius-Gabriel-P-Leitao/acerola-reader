use sqlx::SqlitePool;

use crate::{
    data::{
        models::views::comic_summary_view::ComicSummaryView,
        repositories::views::comic_summary_repo::HomeRepository,
    },
    infra::error::messages::comic_error::ComicError,
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
