use std::path::PathBuf;
use tauri::State;
use sqlx::SqlitePool;

use crate::core::services::comic_scanner_engine::ComicScannerService;

/// Inicia o scan de uma pasta de quadrinhos e persiste os dados encontrados.
///
/// Recebido pelo webview via `invoke("comic_scanner", { path })`.
#[tauri::command]
pub async fn comic_scanner(
    path: String,
    pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);

    let service = ComicScannerService::new(root.clone(), pool.inner().clone());
    service.scan(root).await
}
