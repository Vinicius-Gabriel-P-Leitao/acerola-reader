use tauri::State;

use crate::{
    cmd::events::reader::{
        ReaderChapterPayload, ReaderPagePayload, ReaderSessionPayload, ReaderStatusPayload,
    },
    core::services::reader::ReaderService,
};

#[tauri::command]
pub async fn reader_open_chapter(
    chapter: ReaderChapterPayload, reader: State<'_, ReaderService>,
) -> Result<ReaderSessionPayload, String> {
    reader.open_chapter(chapter).await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn reader_load_page(
    index: usize, set_current: Option<bool>, reader: State<'_, ReaderService>,
) -> Result<ReaderPagePayload, String> {
    reader.load_page(index, set_current.unwrap_or(true)).await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn reader_set_current_page(
    index: usize, reader: State<'_, ReaderService>,
) -> Result<ReaderStatusPayload, String> {
    reader.set_current_page(index).await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn reader_status(
    reader: State<'_, ReaderService>,
) -> Result<ReaderStatusPayload, String> {
    reader.status().await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn reader_close_chapter(
    reader: State<'_, ReaderService>,
) -> Result<ReaderStatusPayload, String> {
    reader.close_chapter().await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn reader_prefetch_window(
    center: usize, radius: Option<usize>, reader: State<'_, ReaderService>,
) -> Result<(), String> {
    reader.prefetch_window_background(center, radius);
    Ok(())
}
