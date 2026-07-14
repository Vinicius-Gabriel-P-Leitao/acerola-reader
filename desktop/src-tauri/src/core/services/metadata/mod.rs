use std::collections::HashMap;
use std::path::PathBuf;
use sqlx::SqlitePool;

use crate::data::{
    models::{
        metadata::{
            author::AuthorMetadata,
            banner::Banner,
            chapter::ChapterMetadata,
            comic::ComicMetadata,
            cover::Cover,
        },
        relations::chapter_with_volume::ChapterArchiveWithVolume,
    },
    repositories::{
        archive::{
            chapter_archive_repo::{ChapterRepository, ChapterSortCriteria},
            comic_directory_repo::ComicRepository,
        },
        metadata::MetadataRepository,
    },
};
use crate::infra::{
    api::{
        anilist::{AnilistClient, AnilistMedia},
        mangadex::{ChapterData, MangaData, MangadexClient, Relationship},
    },
    error::ComicError,
};

pub mod comic_info;

pub struct MetadataService {
    repo: MetadataRepository,
    comic_directory_repo: ComicRepository,
    archive_chapter_repo: ChapterRepository,
    mangadex_client: MangadexClient,
    anilist_client: AnilistClient,
    http_client: reqwest::Client,
}

impl MetadataService {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            repo: MetadataRepository::new(pool.clone()),
            comic_directory_repo: ComicRepository::new(pool.clone()),
            archive_chapter_repo: ChapterRepository::new(pool),
            mangadex_client: MangadexClient::new(),
            anilist_client: AnilistClient::new(),
            http_client: reqwest::Client::builder()
                .user_agent("AcerolaMangaApp/1.0 (Acerola Desktop)")
                .build()
                .unwrap(),
        }
    }

    pub async fn sync_comic_mangadex(&self, title: &str, comic_directory_fk: i64, language: &str, generate_comic_info: bool) -> Result<ComicMetadata, ComicError> {
        let manga = self.fetch_manga_from_mangadex(title).await?;
        let metadata = self.build_metadata_from_mangadex(&manga, comic_directory_fk, language).await?;
        let saved = self.upsert_metadata(metadata, comic_directory_fk).await?;
        
        self.process_mangadex_author(&manga.relationships, saved.id).await?;
        self.process_mangadex_cover(&manga, comic_directory_fk, saved.id).await?;
        self.process_mangadex_chapters(&manga.id, comic_directory_fk, saved.id, language).await?;

        if generate_comic_info {
            self.generate_and_save_comic_info(comic_directory_fk, saved.id).await?;
        }

        Ok(saved)
    }

    pub async fn sync_comic_anilist(&self, title: &str, comic_directory_fk: i64, _language: &str, generate_comic_info: bool) -> Result<ComicMetadata, ComicError> {
        let media = self.fetch_media_from_anilist(title).await?;
        let metadata = self.build_metadata_from_anilist(&media, comic_directory_fk).await?;
        let saved = self.upsert_metadata(metadata, comic_directory_fk).await?;

        self.process_anilist_author(&media, saved.id).await?;
        self.process_anilist_images(&media, comic_directory_fk, saved.id).await?;
        self.process_anilist_chapters(comic_directory_fk, saved.id).await?;

        if generate_comic_info {
            self.generate_and_save_comic_info(comic_directory_fk, saved.id).await?;
        }

        Ok(saved)
    }

    pub async fn parse_and_sync_comic_info(&self, xml_content: &str, comic_directory_fk: i64) -> Result<ComicMetadata, ComicError> {
        let comic_info = quick_xml::de::from_str::<comic_info::ComicInfo>(xml_content)?;
        let metadata = self.build_metadata_from_comic_info(&comic_info, comic_directory_fk).await?;
        let saved = self.upsert_metadata(metadata, comic_directory_fk).await?;

        self.process_comic_info_author(&comic_info, saved.id).await?;
        self.process_comic_info_chapter(&comic_info, saved.id).await?;

        Ok(saved)
    }
}

impl MetadataService {
    fn normalize_name(name: &str) -> String {
        name.chars()
            .filter(|char| char.is_alphanumeric())
            .collect::<String>()
            .to_lowercase()
    }

    async fn fetch_manga_from_mangadex(&self, title: &str) -> Result<MangaData, ComicError> {
        let response = self.mangadex_client.search_manga_by_title(title).await?;
        let normalized_input = Self::normalize_name(title);
        
        tracing::info!("Searching for manga with title: '{}', normalized: '{}', found {} results", title, normalized_input, response.data.len());

        let exact_match = response.data.iter().find(|manga| {
            let titles: Vec<&str> = manga.attributes.title.values()
                .chain(manga.attributes.alt_titles.iter().flat_map(|alt| alt.values()))
                .map(|text| text.as_str())
                .collect();

            titles.iter().any(|text| Self::normalize_name(text) == normalized_input)
        });

        match exact_match {
            Some(manga) => {
                tracing::info!("Found exact match for '{}': {} - {}", title, manga.id, manga.attributes.title.values().next().unwrap_or(&String::new()));
                Ok(manga.clone())
            }
            None => {
                let fallback = response.data.into_iter().next();
                match fallback {
                    Some(manga) => {
                        tracing::warn!("No exact match for '{}', using first result: {} - {}", title, manga.id, manga.attributes.title.values().next().unwrap_or(&String::new()));
                        Ok(manga)
                    }
                    None => Err(ComicError::NotFound)
                }
            }
        }
    }

    async fn fetch_media_from_anilist(&self, title: &str) -> Result<AnilistMedia, ComicError> {
        let response = self.anilist_client.search_manga_by_title(title).await?;
        let normalized_input = Self::normalize_name(title);

        let exact_match = response.data.page.media.iter().find(|media| {
            let titles = vec![
                media.title.user_preferred.as_deref(),
                media.title.english.as_deref(),
                media.title.romaji.as_deref(),
            ];

            titles.into_iter().flatten().any(|text| Self::normalize_name(text) == normalized_input)
        });

        match exact_match {
            Some(media) => Ok(media.clone()),
            None => response.data.page.media.into_iter().next().ok_or(ComicError::NotFound)
        }
    }

    async fn build_metadata_from_mangadex(&self, manga: &MangaData, comic_directory_fk: i64, language: &str) -> Result<ComicMetadata, ComicError> {
        let lang_code = language.split('-').next().unwrap_or(language);
        
        Ok(ComicMetadata {
            id: self.repo.comic_repo.get_next_id().await?,
            title: Self::extract_localized_text(&manga.attributes.title, language, lang_code),
            description: Self::extract_localized_text(&manga.attributes.description, language, lang_code),
            romanji: manga.attributes.title.get("ja-ro").or(manga.attributes.title.get("en")).cloned().unwrap_or_default(),
            status: manga.attributes.status.clone(),
            publication: manga.attributes.year,
            sync_source: Some("MangaDex".to_string()),
            has_comic_info: false,
            comic_directory_fk: Some(comic_directory_fk),
        })
    }

    async fn build_metadata_from_anilist(&self, media: &AnilistMedia, comic_directory_fk: i64) -> Result<ComicMetadata, ComicError> {
        Ok(ComicMetadata {
            id: self.repo.comic_repo.get_next_id().await?,
            title: media.title.user_preferred.clone()
                .or(media.title.english.clone())
                .or(media.title.romaji.clone())
                .unwrap_or_default(),
            description: media.description.clone().unwrap_or_default(),
            romanji: media.title.romaji.clone().unwrap_or_default(),
            status: media.status.clone().unwrap_or_default(),
            publication: None,
            sync_source: Some("AniList".to_string()),
            has_comic_info: false,
            comic_directory_fk: Some(comic_directory_fk),
        })
    }

    async fn generate_and_save_comic_info(&self, comic_directory_fk: i64, metadata_id: i64) -> Result<(), ComicError> {
        let comic_dir = self.comic_directory_repo.find_by_id(comic_directory_fk).await?.ok_or(ComicError::NotFound)?;
        let metadata = self.repo.get_comic_metadata_by_comic_id(comic_directory_fk).await?.ok_or(ComicError::NotFound)?;
        
        let authors = self.repo.get_author_metadata_by_comic_metadata_id(metadata_id).await.unwrap_or_default();
        let writer = authors.iter().map(|a| a.name.clone()).collect::<Vec<_>>().join(", ");
        
        let comic_info = comic_info::ComicInfo {
            title: Some(metadata.title),
            series: None,
            number: None,
            summary: Some(metadata.description),
            writer: if writer.is_empty() { None } else { Some(writer) },
            penciller: None,
            inker: None,
            colorist: None,
            letterer: None,
            cover_artist: None,
            editor: None,
            publisher: None,
            genre: None,
            web: None,
            page_count: None,
            language_iso: None,
            format: None,
            black_and_white: None,
            manga: None,
            characters: None,
            teams: None,
            locations: None,
            scan_information: None,
            story_arc: None,
            series_group: None,
            age_rating: None,
            pages: None,
        };

        let xml_str = quick_xml::se::to_string(&comic_info).map_err(|e| ComicError::SystemFailure(e.to_string()))?;
        let xml_str = xml_str.replace("<ComicInfo>", "<ComicInfo xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        let xml_final = format!("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n{}", xml_str);
        
        let file_path = PathBuf::from(&comic_dir.path).join("ComicInfo.xml");
        std::fs::write(&file_path, xml_final).map_err(|e| ComicError::SystemFailure(e.to_string()))?;

        Ok(())
    }

    async fn build_metadata_from_comic_info(&self, comic_info: &comic_info::ComicInfo, comic_directory_fk: i64) -> Result<ComicMetadata, ComicError> {
        Ok(ComicMetadata {
            id: self.repo.comic_repo.get_next_id().await?,
            title: comic_info.title.clone().unwrap_or_default(),
            description: comic_info.summary.clone().unwrap_or_default(),
            romanji: String::new(),
            status: String::new(),
            publication: None,
            sync_source: Some("ComicInfo".to_string()),
            has_comic_info: true,
            comic_directory_fk: Some(comic_directory_fk),
        })
    }

    async fn upsert_metadata(&self, mut metadata: ComicMetadata, comic_directory_fk: i64) -> Result<ComicMetadata, ComicError> {
        let existing = self.repo.get_comic_metadata_by_comic_id(comic_directory_fk).await?;
        
        tracing::info!("Upsert metadata for comic_directory_fk: {}, existing: {:?}", comic_directory_fk, existing.is_some());
        
        match existing {
            Some(found) => {
                tracing::info!("Updating existing metadata with id: {}", found.id);
                metadata.id = found.id;
                Ok(self.repo.comic_repo.update(&metadata).await?)
            }
            None => {
                tracing::info!("Inserting new metadata");
                Ok(self.repo.comic_repo.insert(&metadata).await?)
            }
        }
    }

    fn extract_localized_text(map: &HashMap<String, String>, language: &str, fallback: &str) -> String {
        map.get(language)
            .or_else(|| map.get("en"))
            .or_else(|| map.get(fallback))
            .or_else(|| map.values().next())
            .cloned()
            .unwrap_or_default()
    }
}

impl MetadataService {
    async fn process_mangadex_author(&self, relationships: &[Relationship], metadata_id: i64) -> Result<(), ComicError> {
        let author_name = relationships.iter()
            .find(|rel| rel.kind == "author")
            .and_then(|rel| rel.attributes.as_ref())
            .and_then(|attr| attr.name.clone());

        if let Some(name) = author_name {
            self.upsert_author(&name, "author", metadata_id).await?;
        }
        Ok(())
    }

    async fn process_anilist_author(&self, media: &AnilistMedia, metadata_id: i64) -> Result<(), ComicError> {
        let author_edge = media.staff.as_ref()
            .and_then(|staff| staff.edges.iter().find(|edge| {
                let role = edge.role.to_lowercase();
                role.contains("story") || role.contains("art") || role.contains("author")
            }));

        if let Some(edge) = author_edge {
            self.upsert_author(&edge.node.name.full, &edge.role, metadata_id).await?;
        }
        Ok(())
    }

    async fn process_comic_info_author(&self, comic_info: &comic_info::ComicInfo, metadata_id: i64) -> Result<(), ComicError> {
        if let Some(writer) = comic_info.writer.as_ref().filter(|text| !text.trim().is_empty()) {
            self.upsert_author(writer, "writer", metadata_id).await?;
        }
        Ok(())
    }

    async fn upsert_author(&self, name: &str, author_type: &str, metadata_id: i64) -> Result<(), ComicError> {
        let existing = self.repo.get_author_metadata_by_comic_metadata_id(metadata_id).await?
            .into_iter()
            .find(|author| author.name == name && author.r#type == author_type);

        match existing {
            Some(found) => {
                let author = AuthorMetadata {
                    id: found.id,
                    name: name.to_string(),
                    r#type: author_type.to_string(),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.author_repo.update(&author).await?;
            }
            None => {
                let author = AuthorMetadata {
                    id: self.repo.author_repo.get_next_id().await?,
                    name: name.to_string(),
                    r#type: author_type.to_string(),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.author_repo.insert(&author).await?;
            }
        }
        Ok(())
    }

    async fn save_author(&self, name: &str, author_type: &str, metadata_id: i64) -> Result<(), ComicError> {
        self.upsert_author(name, author_type, metadata_id).await
    }

    async fn process_comic_info_chapter(&self, comic_info: &comic_info::ComicInfo, metadata_id: i64) -> Result<(), ComicError> {
        if let Some(number) = comic_info.number.as_ref().filter(|text| !text.trim().is_empty()) {
            self.save_chapter(comic_info.title.clone(), number, comic_info.page_count, metadata_id).await?;
        }
        Ok(())
    }

    async fn save_chapter(&self, title: Option<String>, number: &str, page_count: Option<i32>, metadata_id: i64) -> Result<(), ComicError> {
        let existing_chapters = self.repo.get_chapter_metadata_by_comic_metadata_id(metadata_id).await?;
        let existing = existing_chapters.into_iter().find(|c| c.chapter == number);

        match existing {
            Some(found) => {
                let chapter = ChapterMetadata {
                    id: found.id,
                    title: title.or(found.title),
                    chapter: number.to_string(),
                    page_count: page_count.map(|count| count as i64).or(found.page_count),
                    scanlation: found.scanlation,
                    comic_metadata_fk: metadata_id,
                };
                self.repo.chapter_repo.update(&chapter).await?;
            }
            None => {
                let chapter = ChapterMetadata {
                    id: self.repo.chapter_repo.get_next_id().await?,
                    title,
                    chapter: number.to_string(),
                    page_count: page_count.map(|count| count as i64),
                    scanlation: None,
                    comic_metadata_fk: metadata_id,
                };
                self.repo.chapter_repo.insert(&chapter).await?;
            }
        }
        Ok(())
    }
}

impl MetadataService {
    async fn process_mangadex_cover(&self, manga: &MangaData, comic_directory_fk: i64, metadata_id: i64) -> Result<(), ComicError> {
        let comic_dir = self.comic_directory_repo.find_by_id(comic_directory_fk).await?.ok_or(ComicError::NotFound)?;
        
        let file_name = manga.relationships.iter()
            .find(|rel| rel.kind == "cover_art")
            .and_then(|rel| rel.attributes.as_ref())
            .and_then(|attr| attr.file_name.clone())
            .ok_or(ComicError::NotFound)?;

        let cover_url = MangadexClient::get_cover_url(&manga.id, &file_name);
        self.download_and_save_image(&cover_url, &comic_dir.path, "cover.jpg").await?;
        self.upsert_cover_metadata(&cover_url, metadata_id).await?;
        
        let mut updated_dir = comic_dir.clone();
        updated_dir.cover = Some(PathBuf::from(&comic_dir.path).join("cover.jpg").to_string_lossy().to_string());
        self.comic_directory_repo.update(&updated_dir).await?;

        Ok(())
    }

    async fn process_anilist_images(&self, media: &AnilistMedia, comic_directory_fk: i64, metadata_id: i64) -> Result<(), ComicError> {
        let comic_dir = self.comic_directory_repo.find_by_id(comic_directory_fk).await?.ok_or(ComicError::NotFound)?;

        let cover_url = media.cover_image.as_ref()
            .and_then(|cover| cover.large.clone());

        if let Some(url) = cover_url {
            self.download_and_save_image(&url, &comic_dir.path, "cover.jpg").await?;
            self.upsert_cover_metadata(&url, metadata_id).await?;
            
            let mut updated_dir = comic_dir.clone();
            updated_dir.cover = Some(PathBuf::from(&comic_dir.path).join("cover.jpg").to_string_lossy().to_string());
            self.comic_directory_repo.update(&updated_dir).await?;
        }

        if let Some(ref banner_url) = media.banner_image {
            self.download_and_save_image(banner_url, &comic_dir.path, "banner.jpg").await?;
            self.upsert_banner_metadata(banner_url, metadata_id).await?;
            
            let mut updated_dir = comic_dir.clone();
            updated_dir.banner = Some(PathBuf::from(&comic_dir.path).join("banner.jpg").to_string_lossy().to_string());
            self.comic_directory_repo.update(&updated_dir).await?;
        }

        Ok(())
    }

    async fn download_and_save_image(&self, url: &str, dir_path: &str, file_name: &str) -> Result<(), ComicError> {
        let file_path = PathBuf::from(dir_path).join(file_name);

        let response = self.http_client.get(url)
            .header("Accept", "image/*")
            .send().await?
            .error_for_status()
            .map_err(|error| ComicError::SystemFailure(format!("HTTP error: {}", error)))?;

        let bytes = response.bytes().await?;
        
        tracing::info!("Downloaded {} bytes from {}", bytes.len(), url);

        tokio::fs::write(&file_path, &bytes).await?;
        
        Ok(())
    }

    async fn upsert_cover_metadata(&self, url: &str, metadata_id: i64) -> Result<(), ComicError> {
        let existing = self.repo.get_cover_by_comic_metadata_id(metadata_id).await?;
        
        tracing::info!("Upsert cover for metadata_id: {}, existing: {:?}", metadata_id, existing.is_some());
        
        match existing {
            Some(found) => {
                tracing::info!("Updating existing cover with id: {}", found.id);
                let cover = Cover {
                    id: found.id,
                    file_name: "cover.jpg".to_string(),
                    url: url.to_string(),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.cover_repo.update(&cover).await?;
            }
            None => {
                let next_id = self.repo.cover_repo.get_next_id().await?;
                tracing::info!("Inserting new cover with id: {}", next_id);
                let cover = Cover {
                    id: next_id,
                    file_name: "cover.jpg".to_string(),
                    url: url.to_string(),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.cover_repo.insert(&cover).await?;
            }
        }
        Ok(())
    }

    async fn upsert_banner_metadata(&self, url: &str, metadata_id: i64) -> Result<(), ComicError> {
        let existing = self.repo.get_banner_by_comic_metadata_id(metadata_id).await?;
        
        match existing {
            Some(found) => {
                let banner = Banner {
                    id: found.id,
                    file_name: "banner.jpg".to_string(),
                    url: url.to_string(),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.banner_repo.update(&banner).await?;
            }
            None => {
                let banner = Banner {
                    id: self.repo.banner_repo.get_next_id().await?,
                    file_name: "banner.jpg".to_string(),
                    url: url.to_string(),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.banner_repo.insert(&banner).await?;
            }
        }
        Ok(())
    }

    async fn save_cover_metadata(&self, url: &str, metadata_id: i64) -> Result<(), ComicError> {
        self.upsert_cover_metadata(url, metadata_id).await
    }

    async fn save_banner_metadata(&self, url: &str, metadata_id: i64) -> Result<(), ComicError> {
        self.upsert_banner_metadata(url, metadata_id).await
    }
}

impl MetadataService {
    async fn process_mangadex_chapters(&self, manga_id: &str, comic_directory_fk: i64, metadata_id: i64, language: &str) -> Result<(), ComicError> {
        let chapters_response = self.mangadex_client.get_manga_chapters(manga_id, language).await?;
        let local_chapters = self.archive_chapter_repo.get_chapters_by_directory(
            comic_directory_fk, 10000, 0, ChapterSortCriteria::NumberAsc
        ).await?;

        let remote_map = Self::build_chapter_map(chapters_response.data);
        self.sync_matching_chapters(local_chapters, &remote_map, metadata_id).await?;

        Ok(())
    }

    async fn process_anilist_chapters(&self, comic_directory_fk: i64, metadata_id: i64) -> Result<(), ComicError> {
        let local_chapters = self.archive_chapter_repo.get_chapters_by_directory(
            comic_directory_fk, 10000, 0, ChapterSortCriteria::NumberAsc
        ).await?;

        for local_chapter in local_chapters {
            self.save_chapter(None, &local_chapter.chapter, None, metadata_id).await?;
        }

        Ok(())
    }

    fn build_chapter_map(chapters: Vec<ChapterData>) -> HashMap<String, ChapterData> {
        chapters.into_iter()
            .filter_map(|chapter_data| {
                chapter_data.attributes.chapter.clone().map(|chapter_number| (chapter_number, chapter_data))
            })
            .collect()
    }

    async fn sync_matching_chapters(&self, local_chapters: Vec<ChapterArchiveWithVolume>, remote_map: &HashMap<String, ChapterData>, metadata_id: i64) -> Result<(), ComicError> {
        for local_chapter in local_chapters {
            if let Some(remote) = remote_map.get(&local_chapter.chapter_sort) {
                self.save_chapter_metadata(remote, &local_chapter, metadata_id).await?;
            }
        }
        Ok(())
    }

    async fn save_chapter_metadata(&self, remote: &ChapterData, local_chapter: &ChapterArchiveWithVolume, metadata_id: i64) -> Result<(), ComicError> {
        let scanlation = remote.relationships.iter()
            .find(|rel| rel.kind == "scanlation_group")
            .and_then(|rel| rel.attributes.as_ref())
            .and_then(|attr| attr.name.clone());

        let existing_chapters = self.repo.get_chapter_metadata_by_comic_metadata_id(metadata_id).await?;
        let existing = existing_chapters.into_iter().find(|c| c.chapter == local_chapter.chapter);

        match existing {
            Some(found) => {
                let chapter_metadata = ChapterMetadata {
                    id: found.id,
                    title: remote.attributes.title.clone().or(found.title),
                    chapter: local_chapter.chapter.clone(),
                    page_count: remote.attributes.pages.map(|page| page as i64).or(found.page_count),
                    scanlation: scanlation.or(found.scanlation),
                    comic_metadata_fk: metadata_id,
                };
                self.repo.chapter_repo.update(&chapter_metadata).await?;
            }
            None => {
                let chapter_metadata = ChapterMetadata {
                    id: self.repo.chapter_repo.get_next_id().await?,
                    title: remote.attributes.title.clone(),
                    chapter: local_chapter.chapter.clone(),
                    page_count: remote.attributes.pages.map(|page| page as i64),
                    scanlation,
                    comic_metadata_fk: metadata_id,
                };
                self.repo.chapter_repo.insert(&chapter_metadata).await?;
            }
        }

        Ok(())
    }
}
