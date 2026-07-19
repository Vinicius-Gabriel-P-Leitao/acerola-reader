use crate::infra::error::ComicError;
use crate::{
    cmd::events::{metadata::ComicMetadataEvent, shared::ErrorPayload},
    core::services::metadata::MetadataService,
    data::repositories::metadata::MetadataRepository,
};
use std::sync::Arc;
use tauri::{command, State};

pub struct MetadataState {
    pub service: Arc<MetadataService>,
    pub repo: MetadataRepository,
}

#[command]
pub async fn sync_metadata_mangadex(
    title: String, comic_id: String, language: String, generate_comic_info: bool,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let parsed_id = comic_id.parse::<i64>().map_err(|err| {
        ErrorPayload::from(&ComicError::SystemFailure(format!("Invalid ID: {}", err)))
    })?;
    let metadata = state
        .service
        .sync_comic_mangadex(&title, parsed_id, &language, generate_comic_info)
        .await
        .map_err(|err| ErrorPayload::from(&err))?;

    let cover = state.repo.get_cover_by_comic_metadata_id(metadata.id).await.ok().flatten();
    let banner = state.repo.get_banner_by_comic_metadata_id(metadata.id).await.ok().flatten();

    Ok(ComicMetadataEvent::from_model(metadata).with_cover(cover).with_banner(banner))
}

#[command]
pub async fn sync_metadata_anilist(
    title: String, comic_id: String, language: String, generate_comic_info: bool,
    state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let parsed_id = comic_id.parse::<i64>().map_err(|err| {
        ErrorPayload::from(&ComicError::SystemFailure(format!("Invalid ID: {}", err)))
    })?;
    let metadata = state
        .service
        .sync_comic_anilist(&title, parsed_id, &language, generate_comic_info)
        .await
        .map_err(|err| ErrorPayload::from(&err))?;

    let cover = state.repo.get_cover_by_comic_metadata_id(metadata.id).await.ok().flatten();
    let banner = state.repo.get_banner_by_comic_metadata_id(metadata.id).await.ok().flatten();

    Ok(ComicMetadataEvent::from_model(metadata).with_cover(cover).with_banner(banner))
}

#[command]
pub async fn read_comic_info(
    xml_content: String, comic_id: i64, state: State<'_, MetadataState>,
) -> Result<ComicMetadataEvent, ErrorPayload> {
    let metadata = state
        .service
        .parse_and_sync_comic_info(&xml_content, comic_id)
        .await
        .map_err(|err| ErrorPayload::from(&err))?;

    let cover = state.repo.get_cover_by_comic_metadata_id(metadata.id).await.ok().flatten();
    let banner = state.repo.get_banner_by_comic_metadata_id(metadata.id).await.ok().flatten();

    Ok(ComicMetadataEvent::from_model(metadata).with_cover(cover).with_banner(banner))
}

#[command]
pub async fn sync_all_metadata_mangadex<R: tauri::Runtime>(
    language: String, generate_comic_info: bool, app: tauri::AppHandle<R>,
    state: State<'_, MetadataState>,
) -> Result<(), ErrorPayload> {
    use tauri::Emitter;
    let service = state.service.clone();

    tokio::spawn(async move {
        match service
            .sync_all_comics_mangadex(&language, generate_comic_info, |comic_name| {
                let _ = app.emit("metadata:sync_all:progress", comic_name);
            })
            .await
        {
            Ok(_) => {
                let _ = app.emit("metadata:sync_all:complete", ());
            },
            Err(err) => {
                let _ = app.emit("metadata:sync_all:error", ErrorPayload::from(&err));
            },
        }
    });

    Ok(())
}

#[command]
pub async fn sync_all_metadata_anilist<R: tauri::Runtime>(
    language: String, generate_comic_info: bool, app: tauri::AppHandle<R>,
    state: State<'_, MetadataState>,
) -> Result<(), ErrorPayload> {
    use tauri::Emitter;
    let service = state.service.clone();

    tokio::spawn(async move {
        match service
            .sync_all_comics_anilist(&language, generate_comic_info, |comic_name| {
                let _ = app.emit("metadata:sync_all:progress", comic_name);
            })
            .await
        {
            Ok(_) => {
                let _ = app.emit("metadata:sync_all:complete", ());
            },
            Err(err) => {
                let _ = app.emit("metadata:sync_all:error", ErrorPayload::from(&err));
            },
        }
    });

    Ok(())
}
