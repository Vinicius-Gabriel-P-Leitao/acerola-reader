use crate::data::models::views::ComicSummaryView;
use chrono::Local;
use serde::Serialize;
use std::collections::HashMap;

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
    pub chapter_count: i64,
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
    pub fn from(comics: Vec<ComicSummaryView>, counts: HashMap<i64, i64>) -> Self {
        let items = comics
            .into_iter()
            .map(|view| {
                let count = counts.get(&view.directory_id).cloned().unwrap_or(0);
                ComicSummaryItem::from_view(view, count)
            })
            .collect::<Vec<_>>();
        let total = items.len();

        Self {
            total,
            comics: items,
            fetched_at: Local::now().format("%d/%m/%Y %H:%M:%S").to_string(),
        }
    }
}

impl ComicSummaryItem {
    pub fn from_view(view: ComicSummaryView, chapter_count: i64) -> Self {
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
                chapter_count,
            },
            artwork: ComicSummaryArtwork { cover: view.folder_cover, banner: view.folder_banner },
        }
    }
}
