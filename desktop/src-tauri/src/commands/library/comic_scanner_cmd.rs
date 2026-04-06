#[tauri::command]
pub fn comic_scanner(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}
