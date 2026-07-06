use sqlx::SqlitePool;
use tauri::{AppHandle, Emitter, Runtime, State};

use crate::{
    cmd::events::{
        shared::ErrorPayload,
        summary::{ComicSummaryItem, ComicSummaryPayload},
    },
    core::services::summary::{ChapterService, HomeService},
};

#[tauri::command]
pub async fn get_comic_summary<R: Runtime>(
    search: Option<String>, app: AppHandle<R>, pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = HomeService::new(pool);

        match service.get_all(search).await {
            Ok((comics, counts)) => {
                app.emit("home:data", ComicSummaryPayload::from(comics, counts)).unwrap()
            },
            Err(err) => app.emit("home:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}

#[tauri::command]
pub async fn get_comic_by_folder_name(
    folder_name: String, pool: State<'_, SqlitePool>,
) -> Result<Option<ComicSummaryItem>, String> {
    let service = HomeService::new(pool.inner().clone());

    match service.get_by_folder_name(&folder_name).await {
        Ok(Some((view, count))) => Ok(Some(ComicSummaryItem::from_view(view, count))),
        Ok(None) => Ok(None),
        Err(err) => Err(err.to_string()),
    }
}

#[tauri::command]
pub async fn get_comic_chapters<R: Runtime>(
    comic_directory_fk: String, volume_id: Option<String>, page: i32, page_size: i32, asc: bool,
    app: AppHandle<R>, pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let pool = pool.inner().clone();
    tracing::info!("[get_comic_chapters] Called for comic_directory_fk={}, volume_id={:?}, page={}, page_size={}, asc={}", comic_directory_fk, volume_id, page, page_size, asc);

    let comic_directory_id =
        comic_directory_fk.parse::<i64>().map_err(|error| error.to_string())?;
    let volume_id_filter = if let Some(vid) = volume_id {
        Some(vid.parse::<i64>().map_err(|error| error.to_string())?)
    } else {
        None
    };

    tokio::spawn(async move {
        let service = ChapterService::new(pool);

        match service
            .get_comic_chapters(comic_directory_id, volume_id_filter, page, page_size, asc)
            .await
        {
            Ok(data) => {
                tracing::info!(
                    "[get_comic_chapters] Success, emitting comic:chapters with {} items",
                    data.archive.items.len()
                );
                app.emit("comic:chapters", data).unwrap();
            },
            Err(err) => {
                tracing::error!("[get_comic_chapters] Error: {:?}", err);
                app.emit("comic:chapters:error", ErrorPayload::from(&err)).unwrap();
            },
        }
    });

    Ok(())
}
