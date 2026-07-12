use serde::Serialize;

use crate::data::models::metadata::{comic::ComicMetadata, cover::Cover, banner::Banner};

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicMetadataEvent {
    pub id: String,
    pub title: String,
    pub description: String,
    pub romanji: String,
    pub status: String,
    pub publication: Option<i64>,
    pub sync_source: Option<String>,
    pub has_comic_info: bool,
    pub comic_directory_fk: Option<String>,
    pub cover_url: Option<String>,
    pub banner_url: Option<String>,
}

impl ComicMetadataEvent {
    pub fn from_model(model: ComicMetadata) -> Self {
        Self {
            id: model.id.to_string(),
            title: model.title,
            description: model.description,
            romanji: model.romanji,
            status: model.status,
            publication: model.publication,
            sync_source: model.sync_source,
            has_comic_info: model.has_comic_info,
            comic_directory_fk: model.comic_directory_fk.map(|id| id.to_string()),
            cover_url: None,
            banner_url: None,
        }
    }

    pub fn with_cover(mut self, cover: Option<Cover>) -> Self {
        self.cover_url = cover.map(|c| c.url);
        self
    }

    pub fn with_banner(mut self, banner: Option<Banner>) -> Self {
        self.banner_url = banner.map(|b| b.url);
        self
    }
}
