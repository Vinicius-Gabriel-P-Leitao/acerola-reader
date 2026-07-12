use std::collections::HashMap;

use sqlx::SqlitePool;

use crate::{
    data::{
        models::{metadata::comic::ComicMetadata, views::ComicSummaryView},
        repositories::{
            archive::chapter_archive_repo::ChapterRepository,
            metadata::MetadataRepository,
            views::{HomeRepository, SortCriteria},
        },
    },
    infra::error::ComicError,
};

pub struct HomeService {
    repo: HomeRepository,
    chapter_repo: ChapterRepository,
    metadata_repo: MetadataRepository,
}

impl HomeService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { 
            repo: HomeRepository::new(pool.clone()), 
            chapter_repo: ChapterRepository::new(pool.clone()),
            metadata_repo: MetadataRepository::new(pool),
        }
    }

    pub async fn get_all(
        &self, search: Option<String>,
    ) -> Result<(Vec<ComicSummaryView>, HashMap<i64, i64>, HashMap<i64, (ComicMetadata, Option<crate::data::models::metadata::author::AuthorMetadata>)>), ComicError> {
        let comics = match search {
            Some(query) if !query.trim().is_empty() => self.repo.search_by_title(&query).await?,
            _ => self.repo.base.find_all().await?,
        };
        let counts = self.chapter_repo.get_all_counts().await?;
        
        let all_meta = self.metadata_repo.comic_repo.find_all().await?;
        let mut meta_map = HashMap::new();
        for m in all_meta {
            if let Some(fk) = m.comic_directory_fk {
                let author = self.metadata_repo.get_author_metadata_by_comic_metadata_id(m.id).await?.into_iter().next();
                meta_map.insert(fk, (m, author));
            }
        }
        
        Ok((comics, counts, meta_map))
    }

    pub async fn get_all_sorted(
        &self, criteria: SortCriteria,
    ) -> Result<(Vec<ComicSummaryView>, HashMap<i64, i64>, HashMap<i64, (ComicMetadata, Option<crate::data::models::metadata::author::AuthorMetadata>)>), ComicError> {
        let comics = self.repo.find_all_sorted(criteria).await?;
        let counts = self.chapter_repo.get_all_counts().await?;
        
        let all_meta = self.metadata_repo.comic_repo.find_all().await?;
        let mut meta_map = HashMap::new();
        for m in all_meta {
            if let Some(fk) = m.comic_directory_fk {
                let author = self.metadata_repo.get_author_metadata_by_comic_metadata_id(m.id).await?.into_iter().next();
                meta_map.insert(fk, (m, author));
            }
        }
        
        Ok((comics, counts, meta_map))
    }

    pub async fn get_by_folder_name(
        &self, folder_name: &str,
    ) -> Result<Option<(ComicSummaryView, i64, Option<(ComicMetadata, Option<crate::data::models::metadata::author::AuthorMetadata>)>)>, ComicError> {
        let comics = self.repo.base.find_all().await?;
        let comic = comics.into_iter().find(|comic| comic.folder_name == folder_name);

        if let Some(view) = comic {
            let count = self.chapter_repo.count_by_directory_id(view.directory_id).await?;
            let meta = self.metadata_repo.get_comic_metadata_by_comic_id(view.directory_id).await?;
            let author = if let Some(m) = &meta {
                self.metadata_repo.get_author_metadata_by_comic_metadata_id(m.id).await?.into_iter().next()
            } else { None };
            Ok(Some((view, count, meta.map(|m| (m, author)))))
        } else {
            Ok(None)
        }
    }
}
