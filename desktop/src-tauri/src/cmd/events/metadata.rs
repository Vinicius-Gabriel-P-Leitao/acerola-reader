use serde::Serialize;

use crate::data::models::metadata::comic::ComicMetadata;

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
        }
    }
}
