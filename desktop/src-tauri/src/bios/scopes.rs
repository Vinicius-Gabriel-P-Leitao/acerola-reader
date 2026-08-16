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

/// Lê `library_path` diretamente de `settings.json`, sem depender do plugin de store
/// estar inicializado. Usado tanto pelo setup de escopos do FS quanto pelo sync P2P de
/// arquivos, que precisa saber onde gravar capítulos recebidos de outro device.
pub fn read_library_path(app_data_directory: &std::path::Path) -> Option<PathBuf> {
    let settings_file_path = app_data_directory.join("settings.json");
    let file_content = std::fs::read_to_string(&settings_file_path).ok()?;
    extract_library_path(&file_content)
}

/// Lê o override opcional de `relay_url` de `settings.json`. Se ausente (caso comum — a
/// maioria dos usuários nunca mexe nisso), o chamador deve cair no relay padrão hardcoded.
/// Só é lido na inicialização: trocar a URL do relay em runtime não é suportado pela lib,
/// só a troca de modo local/relay (já exposta via `switch_to_local`/`switch_to_relay`).
pub fn read_relay_url_override(app_data_directory: &std::path::Path) -> Option<String> {
    let settings_file_path = app_data_directory.join("settings.json");
    let file_content = std::fs::read_to_string(&settings_file_path).ok()?;
    let json_value: Value = serde_json::from_str(&file_content).ok()?;
    let relay_url = json_value.get("relay_url")?.as_str()?.trim().to_string();

    if relay_url.is_empty() {
        return None;
    }

    Some(relay_url)
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
