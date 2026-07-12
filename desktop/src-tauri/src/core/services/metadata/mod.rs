use sqlx::SqlitePool;

use crate::{
    data::{
        models::metadata::comic::ComicMetadata,
        repositories::metadata::MetadataRepository,
    },
    infra::{
        api::{mangadex::MangadexClient, anilist::AnilistClient},
        error::ComicError,
    },
};

pub mod comic_info;

pub struct MetadataService {
    repo: MetadataRepository,
    archive_chapter_repo: crate::data::repositories::archive::chapter_archive_repo::ChapterRepository,
    mangadex_client: MangadexClient,
    anilist_client: AnilistClient,
}

impl MetadataService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { 
            repo: MetadataRepository::new(pool.clone()),
            archive_chapter_repo: crate::data::repositories::archive::chapter_archive_repo::ChapterRepository::new(pool),
            mangadex_client: MangadexClient::new(),
            anilist_client: AnilistClient::new(),
        }
    }

    pub async fn sync_comic_mangadex(&self, title: &str, comic_directory_fk: i64, language: &str) -> Result<ComicMetadata, ComicError> {
        let res = self.mangadex_client.search_manga_by_title(title).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?;
        
        if let Some(manga) = res.data.first() {
            let next_id = self.repo.comic_repo.get_next_id().await.unwrap_or(0);
            let lang = language.split('-').next().unwrap_or(language); // mangadex might use "pt-br" or "en", wait, title uses 'en' or 'ja'
            let title_str = manga.attributes.title.get(language).or_else(|| manga.attributes.title.get("en")).or_else(|| manga.attributes.title.get(lang)).or_else(|| manga.attributes.title.values().next()).cloned().unwrap_or_default();
            let desc_str = manga.attributes.description.get(language).or_else(|| manga.attributes.description.get("en")).or_else(|| manga.attributes.description.get(lang)).or_else(|| manga.attributes.description.values().next()).cloned().unwrap_or_default();
            
            let mut metadata = ComicMetadata {
                id: next_id,
                title: title_str,
                description: desc_str,
                romanji: manga.attributes.title.get("ja-ro").or_else(|| manga.attributes.title.get("en")).cloned().unwrap_or_default(),
                status: manga.attributes.status.clone(),
                publication: manga.attributes.year.clone(),
                sync_source: Some("MangaDex".to_string()),
                has_comic_info: false,
                comic_directory_fk: Some(comic_directory_fk),
            };

            let saved_meta = if let Ok(Some(existing)) = self.repo.get_comic_metadata_by_comic_id(comic_directory_fk).await {
                metadata.id = existing.id;
                self.repo.comic_repo.update(&metadata).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?
            } else {
                self.repo.comic_repo.insert(&metadata).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?
            };

            let mut author_name = String::new();
            for rel in &manga.relationships {
                if rel.kind == "author" {
                    if let Some(attr) = &rel.attributes {
                        if let Some(name) = &attr.name {
                            author_name = name.clone();
                            break;
                        }
                    }
                }
            }

            if !author_name.is_empty() {
                let author_id = self.repo.author_repo.get_next_id().await.unwrap_or(0);
                let author = crate::data::models::metadata::author::AuthorMetadata {
                    id: author_id,
                    name: author_name,
                    r#type: "author".to_string(),
                    comic_metadata_fk: saved_meta.id,
                };
                let _ = self.repo.author_repo.insert(&author).await;
            }

            if let Ok(chapters_res) = self.mangadex_client.get_manga_chapters(&manga.id, language).await {
                if let Ok(local_chapters) = self.archive_chapter_repo.get_chapters_by_directory(comic_directory_fk, 10000, 0, crate::data::repositories::archive::chapter_archive_repo::ChapterSortCriteria::NumberAsc).await {
                    let mut remote_chapters_map = std::collections::HashMap::new();
                    for chapter_data in chapters_res.data {
                        if let Some(ch_num) = &chapter_data.attributes.chapter {
                            remote_chapters_map.insert(ch_num.clone(), chapter_data);
                        }
                    }

                    for local in local_chapters {
                        let local_num = local.chapter_sort.clone();
                        if let Some(remote) = remote_chapters_map.get(&local_num) {
                            let mut scanlation = None;
                            for rel in &remote.relationships {
                                if rel.kind == "scanlation_group" {
                                    if let Some(attr) = &rel.attributes {
                                        scanlation = attr.name.clone();
                                    }
                                }
                            }

                            let chapter_id = self.repo.chapter_repo.get_next_id().await.unwrap_or(0);
                            let chapter_meta = crate::data::models::metadata::chapter::ChapterMetadata {
                                id: chapter_id,
                                title: remote.attributes.title.clone(),
                                chapter: local.chapter.clone(),
                                page_count: remote.attributes.pages.map(|p| p as i64),
                                scanlation,
                                comic_metadata_fk: saved_meta.id,
                            };
                            let _ = self.repo.chapter_repo.insert(&chapter_meta).await;
                        }
                    }
                }
            }

            return Ok(saved_meta);
        }
        
        Err(ComicError::NotFound)
    }

    pub async fn sync_comic_anilist(&self, title: &str, comic_directory_fk: i64, language: &str) -> Result<ComicMetadata, ComicError> {
        let res = self.anilist_client.search_manga_by_title(title).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?;
        let media = res.data.media;

        let next_id = self.repo.comic_repo.get_next_id().await.unwrap_or(0);
        let mut metadata = ComicMetadata {
            id: next_id,
            title: media.title.english.clone().or(media.title.romaji.clone()).unwrap_or_default(),
            description: media.description.unwrap_or_default(),
            romanji: media.title.romaji.unwrap_or_default(),
            status: media.status.unwrap_or_default(),
            publication: None,
            sync_source: Some("AniList".to_string()),
            has_comic_info: false,
            comic_directory_fk: Some(comic_directory_fk),
        };

        let saved_meta = if let Ok(Some(existing)) = self.repo.get_comic_metadata_by_comic_id(comic_directory_fk).await {
            metadata.id = existing.id;
            self.repo.comic_repo.update(&metadata).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?
        } else {
            self.repo.comic_repo.insert(&metadata).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?
        };

        if let Some(staff) = media.staff {
            if let Some(edge) = staff.edges.iter().find(|e| e.role.to_lowercase().contains("story") || e.role.to_lowercase().contains("art") || e.role.to_lowercase().contains("author")) {
                let author_id = self.repo.author_repo.get_next_id().await.unwrap_or(0);
                let author = crate::data::models::metadata::author::AuthorMetadata {
                    id: author_id,
                    name: edge.node.name.full.clone(),
                    r#type: edge.role.clone(),
                    comic_metadata_fk: saved_meta.id,
                };
                let _ = self.repo.author_repo.insert(&author).await;
            }
        }

        return Ok(saved_meta);
    }

    pub async fn parse_and_sync_comic_info(&self, xml_content: &str, comic_directory_fk: i64) -> Result<ComicMetadata, ComicError> {
        let comic_info: comic_info::ComicInfo = quick_xml::de::from_str(xml_content).map_err(|e| ComicError::InvalidRequest(e.to_string()))?;
        
        let next_id = self.repo.comic_repo.get_next_id().await.unwrap_or(0);
        let mut metadata = ComicMetadata {
            id: next_id,
            title: comic_info.title.clone().unwrap_or_default(),
            description: comic_info.summary.clone().unwrap_or_default(),
            romanji: "".to_string(),
            status: "".to_string(),
            publication: None,
            sync_source: Some("ComicInfo".to_string()),
            has_comic_info: true,
            comic_directory_fk: Some(comic_directory_fk),
        };

        let saved_meta = if let Ok(Some(existing)) = self.repo.get_comic_metadata_by_comic_id(comic_directory_fk).await {
            metadata.id = existing.id;
            self.repo.comic_repo.update(&metadata).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?
        } else {
            self.repo.comic_repo.insert(&metadata).await.map_err(|e| ComicError::SystemFailure(e.to_string()))?
        };

        if let Some(writer) = &comic_info.writer {
            if !writer.trim().is_empty() {
                let author_id = self.repo.author_repo.get_next_id().await.unwrap_or(0);
                let author = crate::data::models::metadata::author::AuthorMetadata {
                    id: author_id,
                    name: writer.clone(),
                    r#type: "writer".to_string(),
                    comic_metadata_fk: saved_meta.id,
                };
                let _ = self.repo.author_repo.insert(&author).await;
            }
        }

        if let Some(number) = &comic_info.number {
            if !number.trim().is_empty() {
                let chapter_id = self.repo.chapter_repo.get_next_id().await.unwrap_or(0);
                let chapter_meta = crate::data::models::metadata::chapter::ChapterMetadata {
                    id: chapter_id,
                    title: comic_info.title.clone(),
                    chapter: number.clone(),
                    page_count: comic_info.page_count.map(|c| c as i64),
                    scanlation: None,
                    comic_metadata_fk: saved_meta.id,
                };
                let _ = self.repo.chapter_repo.insert(&chapter_meta).await;
            }
        }

        return Ok(saved_meta);
    }
}
