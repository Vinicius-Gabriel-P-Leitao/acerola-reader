use tauri_plugin_dialog::DialogExt;
use std::path::PathBuf;
use tauri::AppHandle;
use std::sync::mpsc;

#[tauri::command]
pub async fn select_folder(app: AppHandle) -> Result<String, String> {
    let (tx, rx) = mpsc::channel();

    // FIXME: Tratar melhor com ok e is_err
    // prettier-ignore
    app.dialog().file().pick_folder(move |folder| {
            tx.send(folder).unwrap();
        });

    let path = match rx.recv().unwrap() {
        Some(path) => PathBuf::from(path.to_string()),
        None => {
            return Err("No folder selected".to_string());
        }
    };

    Ok(path.to_string_lossy().to_string())
}
