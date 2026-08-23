use std::path::PathBuf;

use serde_json::Value;
use tauri::Manager;
use tauri_plugin_fs::FsExt;

/// Extrai a propriedade `library_path` do conteúdo JSON das configurações.
fn extract_library_path(file_content: &str) -> Option<PathBuf> {
    let json_value: Value = serde_json::from_str(file_content).ok()?;
    let path_str = json_value.get("library_path")?.as_str()?;
    Some(PathBuf::from(path_str))
}

/// Registra as permissões de acesso ao sistema de arquivos no Tauri usando Early Returns (Guard Clauses)
/// para manter a complexidade ciclomática mínima e o código linear.
pub async fn setup_scopes_from_store(app_handle: &tauri::AppHandle) {
    let app_data_directory = match app_handle.path().app_data_dir() {
        Ok(dir) => dir,
        Err(resolution_error) => {
            tracing::error!("[Bios::Scopes] Failed to resolve app_data_dir: {}", resolution_error);
            return;
        },
    };

    let settings_file_path = app_data_directory.join("settings.json");
    let file_content = match std::fs::read_to_string(&settings_file_path) {
        Ok(content) => content,
        Err(_) => {
            tracing::warn!("[Bios::Scopes] settings.json not found at {:?}", settings_file_path);
            return;
        },
    };

    let library_path = match extract_library_path(&file_content) {
        Some(path) => path,
        None => {
            tracing::warn!("[Bios::Scopes] Key 'library_path' missing or invalid in settings.json");
            return;
        },
    };

    tracing::info!("[Bios::Scopes] Registering filesystem scope for {:?}", library_path);

    if let Err(scope_error) = app_handle.fs_scope().allow_directory(&library_path, true) {
        tracing::error!("[Bios::Scopes] Failed to allow directory in fs_scope: {}", scope_error);
    }

    if let Err(scope_error) = app_handle.asset_protocol_scope().allow_directory(&library_path, true)
    {
        tracing::error!(
            "[Bios::Scopes] Failed to allow directory in asset_protocol_scope: {}",
            scope_error
        );
    }
}
