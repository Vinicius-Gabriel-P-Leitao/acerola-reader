use std::sync::Arc;
use tauri::{command, State};

use crate::cmd::events::{metadata::ComicMetadataEvent, shared::ErrorPayload};
use crate::core::services::metadata::MetadataService;
use crate::data::repositories::metadata::MetadataRepository;

pub struct MetadataState {
    pub service: Arc<MetadataService>,
    pub repo: MetadataRepository,
}

#[command]
pub async fn sync_metadata_mangadex(
    title: String, 
    comic_id: String,
    language: String,
    generate_comic_info: bool,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let parsed_id = comic_id.parse::<i64>().map_err(|e| ErrorPayload::from(&crate::infra::error::ComicError::SystemFailure(format!("Invalid ID: {}", e))))?;
    let metadata = state.service
        .sync_comic_mangadex(&title, parsed_id, &language, generate_comic_info)
        .await
        .map_err(|e| ErrorPayload::from(&e))?;
    
    let cover = state.repo.get_cover_by_comic_metadata_id(metadata.id).await.ok().flatten();
    let banner = state.repo.get_banner_by_comic_metadata_id(metadata.id).await.ok().flatten();
    
    Ok(ComicMetadataEvent::from_model(metadata).with_cover(cover).with_banner(banner))
}

#[command]
pub async fn sync_metadata_anilist(
    title: String, 
    comic_id: String,
    language: String,
    generate_comic_info: bool,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let parsed_id = comic_id.parse::<i64>().map_err(|e| ErrorPayload::from(&crate::infra::error::ComicError::SystemFailure(format!("Invalid ID: {}", e))))?;
    let metadata = state.service
        .sync_comic_anilist(&title, parsed_id, &language, generate_comic_info)
        .await
        .map_err(|e| ErrorPayload::from(&e))?;
    
    let cover = state.repo.get_cover_by_comic_metadata_id(metadata.id).await.ok().flatten();
    let banner = state.repo.get_banner_by_comic_metadata_id(metadata.id).await.ok().flatten();
    
    Ok(ComicMetadataEvent::from_model(metadata).with_cover(cover).with_banner(banner))
}

#[command]
pub async fn read_comic_info(
    xml_content: String, 
    comic_id: i64,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let metadata = state.service
        .parse_and_sync_comic_info(&xml_content, comic_id)
        .await
        .map_err(|e| ErrorPayload::from(&e))?;
    
    let cover = state.repo.get_cover_by_comic_metadata_id(metadata.id).await.ok().flatten();
    let banner = state.repo.get_banner_by_comic_metadata_id(metadata.id).await.ok().flatten();
    
    Ok(ComicMetadataEvent::from_model(metadata).with_cover(cover).with_banner(banner))
}
