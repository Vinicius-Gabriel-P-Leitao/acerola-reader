use crate::{
    cmd::events::{shared::ErrorPayload, summary::{ComicSummaryPayload, ComicSummaryItem}},
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
pub async fn get_comic_by_folder_name(
    folder_name: String,
    pool: State<'_, SqlitePool>
) -> Result<Option<ComicSummaryItem>, String> {
    let service = HomeService::new(pool.inner().clone());
    
    match service.get_by_folder_name(&folder_name).await {
        Ok(Some((view, count))) => Ok(Some(ComicSummaryItem::from_view(view, count))),
        Ok(None) => Ok(None),
        Err(err) => Err(err.to_string()),
    }
}

#[tauri::command]
pub async fn get_comic_chapters(
    comic_directory_fk: String, 
    page: i32, 
    page_size: i32, 
    asc: bool,
    app: AppHandle, 
    pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let pool = pool.inner().clone();
    println!("[get_comic_chapters] Called for comic_directory_fk={}, page={}, page_size={}, asc={}", comic_directory_fk, page, page_size, asc);

    let comic_directory_id = comic_directory_fk.parse::<i64>().map_err(|e| e.to_string())?;

    tokio::spawn(async move {
        let service = ChapterService::new(pool);

        match service.get_comic_chapters(comic_directory_id, page, page_size, asc).await {
            Ok(data) => {
                println!("[get_comic_chapters] Success, emitting comic:chapters with {} items", data.archive.items.len());
                app.emit("comic:chapters", data).unwrap();
            },
            Err(err) => {
                eprintln!("[get_comic_chapters] Error: {:?}", err);
                app.emit("comic:chapters:error", ErrorPayload::from(&err)).unwrap();
            },
        }
    });

    Ok(())
}
