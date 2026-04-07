use std::path::PathBuf;
use std::sync::mpsc;
use tauri::State;
use tauri_plugin_dialog::DialogExt;
use sqlx::SqlitePool;

use crate::core::services::comic_scanner_engine::ComicScannerService;

#[tauri::command]
pub async fn select_folder(
    app: tauri::AppHandle,
    pool: State<'_, SqlitePool>,
) -> Result<String, String> {
    let (tx, rx) = mpsc::channel();

    app.dialog().file().pick_folder(move |folder| {
        tx.send(folder).unwrap();
    });

    let path = match rx.recv().unwrap() {
        Some(path) => PathBuf::from(path.to_string()),
        None => return Err("No folder selected".to_string()),
    };

    let service = ComicScannerService::new(path.clone(), pool.inner().clone());
    service.scan(path.clone()).await?;

    Ok(path.to_string_lossy().to_string())
}
