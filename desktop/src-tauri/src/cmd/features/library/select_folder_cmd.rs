use std::{path::PathBuf, sync::mpsc};

use tauri::{AppHandle, Manager};
use tauri_plugin_dialog::DialogExt;
use tauri_plugin_fs::FsExt;

#[tauri::command]
pub async fn select_folder(app: AppHandle) -> Result<String, String> {
    let (tx, rx) = mpsc::channel();

    app.dialog().file().pick_folder(move |folder| {
        tx.send(folder).unwrap();
    });

    let path = match rx.recv().unwrap() {
        Some(path) => PathBuf::from(path.to_string()),
        None => {
            return Err("No folder selected".to_string());
        },
    };

    app.fs_scope()
        .allow_directory(&path, true)
        .map_err(|e| e.to_string())?;

    app.asset_protocol_scope()
        .allow_directory(&path, true)
        .map_err(|e| e.to_string())?;

    Ok(path.to_string_lossy().to_string())
}
