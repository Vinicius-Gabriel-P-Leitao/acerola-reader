use tauri::State;

use crate::{cmd::events::history::ReadingHistoryPayload, core::services::history::HistoryService};

#[tauri::command]
pub async fn history_update_reading(
    comic_id: String, chapter_id: String, last_page: i64, is_completed: bool,
    history_service: State<'_, HistoryService>,
) -> Result<(), String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;
    let chapter_id = chapter_id.parse::<i64>().map_err(|error| error.to_string())?;

    history_service
        .update_progress(comic_id, chapter_id, last_page, is_completed)
        .await
        .map(|_| ())
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_get_all(
    history_service: State<'_, HistoryService>,
) -> Result<Vec<ReadingHistoryPayload>, String> {
    history_service.find_all().await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_get_comic(
    comic_id: String, history_service: State<'_, HistoryService>,
) -> Result<Option<ReadingHistoryPayload>, String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;
    history_service.find_by_comic(comic_id).await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_get_read_chapters(
    comic_id: String, history_service: State<'_, HistoryService>,
) -> Result<Vec<String>, String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;

    let ids =
        history_service.find_read_chapters(comic_id).await.map_err(|error| error.to_string())?;

    Ok(ids.into_iter().map(|id| id.to_string()).collect())
}

#[tauri::command]
pub async fn history_clear(history_service: State<'_, HistoryService>) -> Result<(), String> {
    history_service.clear().await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_mark_chapter_read(
    comic_id: String, chapter_id: String, history_service: State<'_, HistoryService>,
) -> Result<(), String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;
    let chapter_id = chapter_id.parse::<i64>().map_err(|error| error.to_string())?;

    history_service.mark_chapter_read(comic_id, chapter_id).await.map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_unmark_chapter_read(
    comic_id: String, chapter_id: String, history_service: State<'_, HistoryService>,
) -> Result<(), String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;
    let chapter_id = chapter_id.parse::<i64>().map_err(|error| error.to_string())?;

    history_service
        .unmark_chapter_read(comic_id, chapter_id)
        .await
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_mark_chapters_read_batch(
    comic_id: String, chapter_ids: Vec<String>, history_service: State<'_, HistoryService>,
) -> Result<usize, String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;
    let chapter_ids = chapter_ids
        .into_iter()
        .map(|id| id.parse::<i64>().map_err(|error| error.to_string()))
        .collect::<Result<Vec<_>, _>>()?;

    history_service
        .mark_chapters_read_batch(comic_id, &chapter_ids)
        .await
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn history_unmark_chapters_read_batch(
    comic_id: String, chapter_ids: Vec<String>, history_service: State<'_, HistoryService>,
) -> Result<usize, String> {
    let comic_id = comic_id.parse::<i64>().map_err(|error| error.to_string())?;
    let chapter_ids = chapter_ids
        .into_iter()
        .map(|id| id.parse::<i64>().map_err(|error| error.to_string()))
        .collect::<Result<Vec<_>, _>>()?;

    history_service
        .unmark_chapters_read_batch(comic_id, &chapter_ids)
        .await
        .map_err(|error| error.to_string())
}
