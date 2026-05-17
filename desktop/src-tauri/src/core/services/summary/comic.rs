use sqlx::SqlitePool;
use std::collections::HashMap;

use crate::{
    data::{
        models::views::ComicSummaryView,
        repositories::{archive::chapter_archive_repo::ChapterRepository, views::HomeRepository},
    },
    infra::error::ComicError,
};

pub struct HomeService {
    repo: HomeRepository,
    chapter_repo: ChapterRepository,
}

impl HomeService {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            repo: HomeRepository::new(pool.clone()),
            chapter_repo: ChapterRepository::new(pool),
        }
    }

    pub async fn get_all(&self) -> Result<(Vec<ComicSummaryView>, HashMap<i64, i64>), ComicError> {
        let comics = self.repo.base.find_all().await?;
        let counts = self.chapter_repo.get_all_counts().await?;
        Ok((comics, counts))
    }
}
