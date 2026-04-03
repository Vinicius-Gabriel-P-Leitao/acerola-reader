use std::sync::mpsc;
use tauri_plugin_dialog::DialogExt;

#[tauri::command]
pub async fn select_folder(app: tauri::AppHandle) -> Result<String, String> {
    let (tx, rx) = mpsc::channel();

    // prettier-ignore
    app.dialog().file()
        .pick_folder(move |folder| {
            tx.send(folder).unwrap();
        });

    match rx.recv().unwrap() {
        Some(path) => Ok(path.to_string()),
        None => Err("Nenhuma pasta selecionada".to_string()),
    }
}
