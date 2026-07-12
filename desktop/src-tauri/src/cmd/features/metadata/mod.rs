use std::sync::Arc;
use tauri::{command, State};

use crate::cmd::events::{metadata::ComicMetadataEvent, shared::ErrorPayload};
use crate::core::services::metadata::MetadataService;

pub struct MetadataState {
    pub service: Arc<MetadataService>,
}

#[command]
pub async fn sync_metadata_mangadex(
    title: String, 
    comic_id: String,
    language: String,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let parsed_id = comic_id.parse::<i64>().map_err(|e| ErrorPayload::from(&crate::infra::error::ComicError::SystemFailure(format!("Invalid ID: {}", e))))?;
    state.service
        .sync_comic_mangadex(&title, parsed_id, &language)
        .await
        .map(ComicMetadataEvent::from_model)
        .map_err(|e| ErrorPayload::from(&e))
}

#[command]
pub async fn sync_metadata_anilist(
    title: String, 
    comic_id: String,
    language: String,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let parsed_id = comic_id.parse::<i64>().map_err(|e| ErrorPayload::from(&crate::infra::error::ComicError::SystemFailure(format!("Invalid ID: {}", e))))?;
    state.service
        .sync_comic_anilist(&title, parsed_id, &language)
        .await
        .map(ComicMetadataEvent::from_model)
        .map_err(|e| ErrorPayload::from(&e))
}

#[command]
pub async fn read_comic_info(
    xml_content: String, 
    comic_id: i64,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    state.service
        .parse_and_sync_comic_info(&xml_content, comic_id)
        .await
        .map(ComicMetadataEvent::from_model)
        .map_err(|e| ErrorPayload::from(&e))
}
