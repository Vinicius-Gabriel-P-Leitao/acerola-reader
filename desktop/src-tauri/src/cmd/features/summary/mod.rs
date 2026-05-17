use crate::{
    cmd::events::{shared::ErrorPayload, summary::ComicSummaryPayload},
    core::services::summary::{HomeService, ChapterService},
};

use sqlx::SqlitePool;
use tauri::{AppHandle, Emitter, State};

#[tauri::command]
pub async fn get_comic_summary(app: AppHandle, pool: State<'_, SqlitePool>) -> Result<(), String> {
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = HomeService::new(pool);

        match service.get_all().await {
            Ok((comics, counts)) => {
                app.emit("home:data", ComicSummaryPayload::from(comics, counts)).unwrap()
            },
            Err(err) => app.emit("home:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}

#[tauri::command]
pub async fn get_comic_chapters(
    comic_directory_fk: i64, 
    page: i32, 
    page_size: i32, 
    asc: bool,
    app: AppHandle, 
    pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ChapterService::new(pool);

        match service.get_comic_chapters(comic_directory_fk, page, page_size, asc).await {
            Ok(data) => app.emit("comic:chapters", data).unwrap(),
            Err(err) => app.emit("comic:chapters:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}
