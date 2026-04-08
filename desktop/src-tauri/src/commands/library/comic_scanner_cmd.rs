use tauri::{ AppHandle, Emitter };
use std::path::PathBuf;
use sqlx::SqlitePool;
use tauri::State;

use crate::core::services::comic_scanner_engine::ComicScannerService;

/// Inicia o scan de uma pasta de quadrinhos e persiste os dados encontrados.
///
/// Recebido pelo webview via `invoke("comic_scanner", { path })`.
#[tauri::command]
pub async fn comic_scanner(
    path: String,
    app: AppHandle,
    pool: State<'_, SqlitePool>
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service.scan(root, &app).await {
            Ok(_) => {
                app.emit("scan:complete", ()).unwrap();
            }
            Err(err) => {
                app.emit("scan:error", err).unwrap();
            }
        }
    });

    Ok(())
}
