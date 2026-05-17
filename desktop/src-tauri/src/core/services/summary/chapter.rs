use sqlx::SqlitePool;
use crate::{
    data::repositories::archive::{
        chapter_archive_repo::{ChapterRepository, ChapterArchiveWithVolume},
        volume_archive_repo::VolumeRepository,
    },
    cmd::events::summary::{
        ChapterDto, ChapterPageDto, ChapterFileDto, VolumeArchiveDto, VolumeChapterGroupDto, VolumeViewType,
    },
    infra::error::ComicError,
};

pub struct ChapterService {
    chapter_repo: ChapterRepository,
    volume_repo: VolumeRepository,
}

impl ChapterService {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            chapter_repo: ChapterRepository::new(pool.clone()),
            volume_repo: VolumeRepository::new(pool),
        }
    }

    pub async fn get_comic_chapters(
        &self, 
        comic_directory_fk: i64, 
        page: i32, 
        page_size: i32,
        asc: bool,
    ) -> Result<ChapterDto, ComicError> {
        let offset = (page * page_size) as i64;
        
        // 1. Fetch all volumes for this comic
        let volumes = self.volume_repo.find_by_comic(comic_directory_fk).await?;
        let has_volume_structure = !volumes.is_empty();
        
        // 2. Fetch paginated chapters (joining volume info)
        let chapters_with_volume = if asc {
            self.chapter_repo
                .get_chapters_by_directory_paged(comic_directory_fk, page_size as i64, offset)
                .await?
        } else {
            self.chapter_repo
                .get_chapters_by_directory_paged_desc(comic_directory_fk, page_size as i64, offset)
                .await?
        };
            
        let total_chapters = self.chapter_repo.count_by_directory_id(comic_directory_fk).await?;
        
        // 3. Map to DTOs
        let chapter_items = chapters_with_volume.iter().map(|chapter| {
            ChapterFileDto {
                id: chapter.id.to_string(),
                name: chapter.chapter.clone(),
                path: chapter.path.clone(),
                chapter_sort: chapter.chapter_sort.clone(),
                volume_id: chapter.volume_id_fk.map(|id| id.to_string()),
                volume_name: chapter.volume_name.clone(),
                is_special: chapter.is_special,
                last_modified: chapter.last_modified,
            }
        }).collect::<Vec<_>>();

        let volume_dtos = volumes.iter().map(|volume| {
            VolumeArchiveDto {
                id: volume.id.to_string(),
                name: volume.name.clone(),
                volume_sort: volume.volume_sort.clone(),
                is_special: volume.is_special,
                cover_uri: volume.cover.clone(),
                banner_uri: volume.banner.clone(),
                last_modified: volume.last_modified,
            }
        }).collect::<Vec<_>>();

        // 4. Group by volume for volume sections (if needed)
        let mut volume_sections = Vec::new();
        if has_volume_structure {
            for volume in &volumes {
                let items_in_vol = chapters_with_volume.iter()
                    .filter(|chapter| chapter.volume_id_fk == Some(volume.id))
                    .map(|chapter| ChapterFileDto {
                        id: chapter.id.to_string(),
                        name: chapter.chapter.clone(),
                        path: chapter.path.clone(),
                        chapter_sort: chapter.chapter_sort.clone(),
                        volume_id: chapter.volume_id_fk.map(|id| id.to_string()),
                        volume_name: chapter.volume_name.clone(),
                        is_special: chapter.is_special,
                        last_modified: chapter.last_modified,
                    })
                    .collect::<Vec<_>>();

                if !items_in_vol.is_empty() {
                    let total_in_vol = self.chapter_repo.get_total_count_by_volume(volume.id).await?;
                    let total_pages_vol = (total_in_vol as f64 / page_size as f64).ceil() as i32;
                    
                    volume_sections.push(VolumeChapterGroupDto {
                        volume: VolumeArchiveDto {
                            id: volume.id.to_string(),
                            name: volume.name.clone(),
                            volume_sort: volume.volume_sort.clone(),
                            is_special: volume.is_special,
                            cover_uri: volume.cover.clone(),
                            banner_uri: volume.banner.clone(),
                            last_modified: volume.last_modified,
                        },
                        items: items_in_vol,
                        total_chapters: total_in_vol as i32,
                        loaded_count: total_chapters as i32, 
                        has_more: (offset + page_size as i64) < total_chapters, 
                        current_page: page,
                        total_pages: total_pages_vol,
                    });
                }
            }
        }

        let effective_view_mode = if has_volume_structure {
            VolumeViewType::Volume
        } else {
            VolumeViewType::Chapter
        };

        Ok(ChapterDto {
            archive: ChapterPageDto {
                items: chapter_items,
                volumes: volume_dtos,
                page_size,
                page,
                total: total_chapters as i32,
                volume_sections,
            },
            show_volume_headers: has_volume_structure,
            has_volume_structure,
            effective_view_mode,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db_with_volumes;
    use crate::data::models::archive::chapter_archive::ChapterArchive;
    use crate::data::repositories::Repository;

    async fn popular_dados(pool: &SqlitePool) {
        let chapter_repo = Repository::<ChapterArchive>::new(pool.clone());
        chapter_repo.insert(&ChapterArchive {
            id: 1,
            chapter: "Cap 1".to_string(),
            path: "p1".to_string(),
            chapter_sort: "1".to_string(),
            is_special: false,
            checksum: None,
            fast_hash: None,
            comic_directory_fk: 1,
            volume_id_fk: Some(1),
            last_modified: 0,
        }).await.unwrap();
    }

    #[tokio::test]
    async fn teste_get_comic_chapters() {
        let pool = setup_test_db_with_volumes().await;
        popular_dados(&pool).await;

        let service = ChapterService::new(pool);
        let result = service.get_comic_chapters(1, 0, 25, true).await.unwrap();

        assert_eq!(result.archive.total, 1);
        assert_eq!(result.archive.items.len(), 1);
        assert!(result.has_volume_structure);
        assert_eq!(result.archive.volume_sections.len(), 1);
        assert_eq!(result.archive.volume_sections[0].volume.name, "Vol 01");
    }
}
