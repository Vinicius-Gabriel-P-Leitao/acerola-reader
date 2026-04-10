use crate::{
    commands::events::shared::scanner_event::ScanErrorPayload,
    core::services::comic_scanner_engine::ComicScannerService,
};

use sqlx::SqlitePool;
use std::path::PathBuf;
use tauri::State;
use tauri::{ AppHandle, Emitter };

/// Inicia o scan de uma pasta de quadrinhos e persiste os dados encontrados.
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
                // FIXME: Verificar se dá pra fazer um contrato no command que eu consiga usar
                app.emit("scan:complete", ()).unwrap();
            }
            Err(err) => {
                // FIXME: Verificar se dá pra fazer um contrato no command que eu consiga usar
                app.emit("scan:error", ScanErrorPayload::from(&err)).unwrap();
            }
        }
    });

    Ok(())
}
