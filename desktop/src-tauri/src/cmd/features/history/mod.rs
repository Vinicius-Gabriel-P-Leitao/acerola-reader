use tauri::State;

use crate::core::services::history::{HistoryService, ReadingHistoryView};

#[tauri::command]
pub async fn history_update_reading(
    comic_id: String,
    chapter_id: String,
    last_page: i64,
    is_completed: bool,
    history_service: State<'_, HistoryService>,
) -> Result<(), String> {
    let comic_id_i64 = comic_id.parse::<i64>().map_err(|e| e.to_string())?;
    let chapter_id_i64 = chapter_id.parse::<i64>().map_err(|e| e.to_string())?;

    history_service
        .update_reading_history(comic_id_i64, chapter_id_i64, last_page, is_completed)
        .await
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub async fn history_get_all(
    history_service: State<'_, HistoryService>,
) -> Result<Vec<ReadingHistoryView>, String> {
    history_service.get_full_history().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn history_get_comic(
    comic_id: String,
    history_service: State<'_, HistoryService>,
) -> Result<Option<ReadingHistoryView>, String> {
    let comic_id_i64 = comic_id.parse::<i64>().map_err(|e| e.to_string())?;
    history_service.get_comic_history_view(comic_id_i64).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn history_get_read_chapters(
    comic_id: String,
    history_service: State<'_, HistoryService>,
) -> Result<Vec<String>, String> {
    let comic_id_i64 = comic_id.parse::<i64>().map_err(|e| e.to_string())?;
    
    let chapters = history_service.get_read_chapters(comic_id_i64)
        .await
        .map_err(|e| e.to_string())?;
        
    Ok(chapters.into_iter().map(|id| id.to_string()).collect())
}

#[tauri::command]
pub async fn history_clear(
    history_service: State<'_, HistoryService>,
) -> Result<(), String> {
    history_service.clear_history().await.map_err(|e| e.to_string())
}
