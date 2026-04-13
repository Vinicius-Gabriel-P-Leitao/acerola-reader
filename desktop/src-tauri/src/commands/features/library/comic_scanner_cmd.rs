use crate::{
    commands::events::shared::error_payload::ErrorPayload,
    core::services::comic_scanner_engine::ComicScannerService,
};
use sqlx::SqlitePool;
use std::path::PathBuf;
use tauri::State;
use tauri::{AppHandle, Emitter};

/// Processa todas as pastas encontradas no disco sem comparar com o banco.
/// Pastas já indexadas são ignoradas via INSERT OR IGNORE.
#[tauri::command]
pub async fn refresh_library(
    path: String,
    app: AppHandle,
    pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service
            .refresh_library(root, |it| {
                let _ = app.emit("scan:progress", it);
            })
            .await
        {
            Ok(_) => app.emit("scan:complete", ()).unwrap(),
            Err(err) => app.emit("scan:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}

/// Processa apenas pastas novas ou modificadas com base no `last_modified`.
/// Remove do banco as pastas que não existem mais no disco.
#[tauri::command]
pub async fn incremental_scan(
    path: String,
    app: AppHandle,
    pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service
            .incremental_scan(root, |it| {
                let _ = app.emit("scan:progress", it);
            })
            .await
        {
            Ok(_) => app.emit("scan:complete", ()).unwrap(),
            Err(err) => app.emit("scan:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}

/// Faz o refresh completo de todas as pastas e re-escaneia os capítulos de cada comic.
/// É a operação mais pesada — use quando o banco pode estar em estado inconsistente.
#[tauri::command]
pub async fn rebuild_library(
    path: String,
    app: AppHandle,
    pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service
            .rebuild_library(root, |it| {
                let _ = app.emit("scan:progress", it);
            })
            .await
        {
            Ok(_) => app.emit("scan:complete", ()).unwrap(),
            Err(err) => app.emit("scan:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}
