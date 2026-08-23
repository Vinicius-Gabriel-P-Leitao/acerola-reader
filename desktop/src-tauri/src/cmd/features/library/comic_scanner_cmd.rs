use std::path::PathBuf;

use sqlx::SqlitePool;
use tauri::{AppHandle, Emitter, Runtime, State};

use crate::{
    cmd::events::shared::ErrorPayload,
    core::services::archive::comic_scanner_engine::ComicScannerService,
};

/// Processa todas as pastas encontradas no disco sem comparar com o banco.
/// Pastas já indexadas são ignoradas via INSERT OR IGNORE.
#[tauri::command]
pub async fn refresh_library<R: Runtime>(
    path: String, app: AppHandle<R>, pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service
            .refresh_library(
                root,
                |progress_path| {
                    if let Err(error) = app.emit("scan:progress", progress_path) {
                        tracing::error!("Failed to emit scan:progress: {}", error);
                    }
                },
                |converting_message| {
                    if let Err(error) = app.emit("scan:converting", converting_message) {
                        tracing::error!("Failed to emit scan:converting: {}", error);
                    }
                },
            )
            .await
        {
            Ok(_) => {
                if let Err(error) = app.emit("scan:complete", ()) {
                    tracing::error!("Failed to emit scan:complete: {}", error);
                }
            },
            Err(error) => {
                if let Err(emit_error) = app.emit("scan:error", ErrorPayload::from(&error)) {
                    tracing::error!("Failed to emit scan:error: {}", emit_error);
                }
            },
        }
    });

    Ok(())
}

/// Processa apenas pastas novas ou modificadas com base no `last_modified`.
/// Remove do banco as pastas que não existem mais no disco.
#[tauri::command]
pub async fn incremental_scan<R: Runtime>(
    path: String, app: AppHandle<R>, pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service
            .incremental_scan(
                root,
                |progress_path| {
                    if let Err(error) = app.emit("scan:progress", progress_path) {
                        tracing::error!("Failed to emit scan:progress: {}", error);
                    }
                },
                |converting_message| {
                    if let Err(error) = app.emit("scan:converting", converting_message) {
                        tracing::error!("Failed to emit scan:converting: {}", error);
                    }
                },
            )
            .await
        {
            Ok(_) => {
                if let Err(error) = app.emit("scan:complete", ()) {
                    tracing::error!("Failed to emit scan:complete: {}", error);
                }
            },
            Err(error) => {
                if let Err(emit_error) = app.emit("scan:error", ErrorPayload::from(&error)) {
                    tracing::error!("Failed to emit scan:error: {}", emit_error);
                }
            },
        }
    });

    Ok(())
}

/// Faz o refresh completo de todas as pastas e re-escaneia os capítulos de cada comic.
/// É a operação mais pesada — use quando o banco pode estar em estado inconsistente.
#[tauri::command]
pub async fn rebuild_library<R: Runtime>(
    path: String, app: AppHandle<R>, pool: State<'_, SqlitePool>,
) -> Result<(), String> {
    let root = PathBuf::from(&path);
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = ComicScannerService::new(root.clone(), pool);

        match service
            .rebuild_library(
                root,
                |progress_path| {
                    if let Err(error) = app.emit("scan:progress", progress_path) {
                        tracing::error!("Failed to emit scan:progress: {}", error);
                    }
                },
                |converting_message| {
                    if let Err(error) = app.emit("scan:converting", converting_message) {
                        tracing::error!("Failed to emit scan:converting: {}", error);
                    }
                },
            )
            .await
        {
            Ok(_) => {
                if let Err(error) = app.emit("scan:complete", ()) {
                    tracing::error!("Failed to emit scan:complete: {}", error);
                }
            },
            Err(error) => {
                if let Err(emit_error) = app.emit("scan:error", ErrorPayload::from(&error)) {
                    tracing::error!("Failed to emit scan:error: {}", emit_error);
                }
            },
        }
    });

    Ok(())
}
