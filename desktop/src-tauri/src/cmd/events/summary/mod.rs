use crate::data::models::views::ComicSummaryView;
use chrono::Local;
use serde::Serialize;

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicSummaryRelations {
    pub directory_id: i64,
    pub metadata_id: Option<i64>,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicSummaryFilesystem {
    pub folder_name: String,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicSummaryMetadata {
    pub title: Option<String>,
    pub external_sync: bool,
    pub active_source: Option<String>,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicSummaryArtwork {
    pub cover: Option<String>,
    pub banner: Option<String>,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicSummaryItem {
    pub relations: ComicSummaryRelations,
    pub filesystem: ComicSummaryFilesystem,
    pub metadata: ComicSummaryMetadata,
    pub artwork: ComicSummaryArtwork,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ComicSummaryPayload {
    pub comics: Vec<ComicSummaryItem>,
    pub total: usize,
    pub fetched_at: String,
}

impl ComicSummaryPayload {
    pub fn from(comics: Vec<ComicSummaryView>) -> Self {
        let items = comics.into_iter().map(ComicSummaryItem::from).collect::<Vec<_>>();
        let total = items.len();

        Self {
            total,
            comics: items,
            fetched_at: Local::now().format("%d/%m/%Y %H:%M:%S").to_string(),
        }
    }
}

impl From<ComicSummaryView> for ComicSummaryItem {
    fn from(view: ComicSummaryView) -> Self {
        Self {
            relations: ComicSummaryRelations {
                directory_id: view.directory_id,
                metadata_id: view.metadata_id,
            },
            filesystem: ComicSummaryFilesystem { folder_name: view.folder_name },
            metadata: ComicSummaryMetadata {
                title: view.metadata_title,
                external_sync: view.external_sync,
                active_source: view.active_source,
            },
            artwork: ComicSummaryArtwork { cover: view.folder_cover, banner: view.folder_banner },
        }
    }
}
