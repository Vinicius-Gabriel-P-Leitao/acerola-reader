use sqlx::SqlitePool;
use tauri::{AppHandle, Emitter, Runtime, State};

use crate::{
    cmd::events::{shared::ErrorPayload, summary::ComicSummaryPayload},
    core::services::{summary::HomeService, ComicService},
    data::repositories::views::SortCriteria,
};

/// Comando Tauri para buscar quadrinhos com ordenação específica.
#[tauri::command]
pub async fn get_comic_summary_sorted<R: Runtime>(
    sort_by: String, sort_order: String, app: AppHandle<R>, pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let pool = pool.inner().clone();

    // Parse do criteria de ordenação
    let criteria = match (sort_by.as_str(), sort_order.as_str()) {
        ("title", "asc") => SortCriteria::TitleAsc,
        ("title", "desc") => SortCriteria::TitleDesc,
        ("chapterCount", "asc") => SortCriteria::ChapterCountAsc,
        ("chapterCount", "desc") => SortCriteria::ChapterCountDesc,
        _ => SortCriteria::TitleAsc,
    };

    tokio::spawn(async move {
        let service = HomeService::new(pool);

        match service.get_all_sorted(criteria).await {
            Ok((comics, counts)) => {
                app.emit("home:data", ComicSummaryPayload::from(comics, counts)).unwrap()
            },
            Err(err) => app.emit("home:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}

/// Comando Tauri para atualizar visibilidade de múltiplos quadrinhos.
#[tauri::command]
pub async fn update_comics_visibility(
    ids: Vec<i64>, hidden: bool, pool: State<'_, SqlitePool>,
) -> Result<usize, ErrorPayload> {
    let service = ComicService::new(pool.inner().clone());
    service
        .update_hidden_status_batch(&ids, hidden)
        .await
        .map_err(|error| ErrorPayload::from(&error))
}

/// Comando Tauri para deletar múltiplos quadrinhos.
#[tauri::command]
pub async fn delete_comics(ids: Vec<i64>, pool: State<'_, SqlitePool>) -> Result<usize, ErrorPayload> {
    let service = ComicService::new(pool.inner().clone());
    service.delete_batch(&ids).await.map_err(|error| ErrorPayload::from(&error))
}
