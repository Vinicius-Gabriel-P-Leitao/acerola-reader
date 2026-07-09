#![allow(dead_code)]

mod cmd;
mod core;
mod data;
mod infra;

use cmd::features::{
    category::category_cmd,
    comic as comic_cmd, history as history_cmd,
    library::{comic_scanner_cmd, select_folder_cmd},
    network as network_cmd, reader as reader_cmd, summary as comic_summary_cmd,
};
use tauri::Manager;

#[cfg(test)]
pub mod tests;

mod app_bootstrap {
    use std::{path::PathBuf, sync::Arc};

    use acerola_p2p::api::{
        guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
        identity::{DefaultDeviceInfoProvider, DeviceInfoProvider},
        transport::IrohTransportBuilder,
        AcerolaP2p,
    };
    use tauri::Emitter;
    use tauri_plugin_fs::FsExt;

    use super::*;
    use crate::core::services::{
        network::{NetworkService, NetworkServiceApi},
        reader::ReaderService,
    };

    pub fn build() -> tauri::Builder<tauri::Wry> {
        let builder = tauri::Builder::default();
        let builder = setup_opener(builder);
        let builder = setup_dialog(builder);
        let builder = setup_store(builder);
        let builder = setup_sql(builder);
        let builder = setup_fs(builder);

        // INFO: Commands que serão chamados via invoke
        builder.setup(setup_runtime).invoke_handler(tauri::generate_handler![
            comic_scanner_cmd::incremental_scan,
            comic_summary_cmd::get_comic_summary,
            comic_summary_cmd::get_comic_by_folder_name,
            comic_summary_cmd::get_comic_chapters,
            comic_scanner_cmd::refresh_library,
            comic_scanner_cmd::rebuild_library,
            select_folder_cmd::select_folder,
            network_cmd::get_network_status,
            network_cmd::switch_to_local,
            network_cmd::switch_to_relay,
            network_cmd::connect_to_peer,
            network_cmd::get_local_id,
            reader_cmd::reader_open_chapter,
            reader_cmd::reader_load_page,
            reader_cmd::reader_set_current_page,
            reader_cmd::reader_status,
            reader_cmd::reader_close_chapter,
            reader_cmd::reader_prefetch_window,
            history_cmd::history_update_reading,
            history_cmd::history_get_all,
            history_cmd::history_get_comic,
            history_cmd::history_get_read_chapters,
            history_cmd::history_clear,
            system_cmd::get_package_family_name,
            category_cmd::create_category,
            category_cmd::get_categories,
            category_cmd::delete_category,
            category_cmd::assign_category_to_comic,
            category_cmd::remove_category_from_comic,
            category_cmd::get_comic_category,
            category_cmd::get_all_comic_categories,
            comic_cmd::get_comic_summary_sorted,
            comic_cmd::update_comics_visibility,
            comic_cmd::delete_comics,
            system_cmd::open_filesystem_access_settings,
        ])
    }

    fn setup_opener(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
        builder.plugin(tauri_plugin_opener::init())
    }

    fn setup_dialog(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
        builder.plugin(tauri_plugin_dialog::init())
    }

    fn setup_fs(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
        builder.plugin(tauri_plugin_fs::init())
    }

    fn setup_store(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
        builder.plugin(tauri_plugin_store::Builder::new().build())
    }

    fn setup_sql(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
        builder.plugin(
            tauri_plugin_sql::Builder::new()
                .add_migrations("sqlite:acerola.db", crate::infra::db::get_migrations())
                .build(),
        )
    }

    async fn setup_database(handle: &tauri::AppHandle, db_path: PathBuf) {
        #[rustfmt::skip]
        let pool = sqlx::SqlitePool::connect(&format!(
            "sqlite:{}?mode=rwc",
            db_path.to_string_lossy()
        )).await.unwrap();

        handle.manage(pool.clone());
        handle.manage(crate::core::services::history::HistoryService::new(pool));
    }

    async fn setup_scopes_from_store(handle: &tauri::AppHandle) {
        let store_path = handle.path().app_data_dir().unwrap().join("settings.json");

        tracing::info!("Procurando settings.json em: {:?}", store_path);

        if !store_path.exists() {
            tracing::warn!("settings.json não encontrado em: {:?}", store_path);
            return;
        }

        if let Ok(content) = std::fs::read_to_string(&store_path) {
            tracing::info!("Conteúdo do settings.json: {}", content);
            if let Ok(json) = serde_json::from_str::<serde_json::Value>(&content) {
                if let Some(path_str) = json.get("library_path").and_then(|v| v.as_str()) {
                    let path = PathBuf::from(path_str);
                    tracing::info!("Registrando scope para: {:?}", path);

                    match handle.fs_scope().allow_directory(&path, true) {
                        Ok(_) => tracing::info!("fs_scope OK"),
                        Err(e) => tracing::error!("fs_scope falhou: {}", e),
                    }
                    match handle.asset_protocol_scope().allow_directory(&path, true) {
                        Ok(_) => tracing::info!("asset_protocol_scope OK"),
                        Err(e) => tracing::error!("asset_protocol_scope falhou: {}", e),
                    }
                } else {
                    tracing::warn!("Chave 'library_path' não encontrada no JSON: {:?}", json);
                }
            }
        }
    }

    async fn setup_network(handle: &tauri::AppHandle) {
        let app = handle.clone();

        let emit: acerola_p2p::api::protocol::EventEmitter = Arc::new(move |event, data| {
            app.emit(event, data).ok();
        });

        let store = Arc::new(InMemoryTrustedStore::new());

        let transport = IrohTransportBuilder::default()
            // TODO: Derivar de forma melhor o valor
            .seed(*b"acerola-desktop-seed-v1-00000000")
            .relay("https://relay.acerola-comic.com");

        let device = DefaultDeviceInfoProvider::new("0.0.1-beta")
            .provide()
            .expect("Failed to read device info");

        let node = AcerolaP2p::builder(emit, transport, device)
            .guard(TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator())
            .build()
            .await
            .expect("Failed to start the p2p node");

        let service: Arc<dyn NetworkServiceApi> = Arc::new(NetworkService::new(Arc::new(node)));
        handle.manage(service);
    }

    fn setup_runtime(app: &mut tauri::App) -> Result<(), Box<dyn std::error::Error>> {
        let handle: tauri::AppHandle = app.handle().clone();
        let (db_path, log_dir) = resolve_paths(app);

        tracing::info!("app_data_dir: {:?}", handle.path().app_data_dir().unwrap());
        tracing::info!("db_path: {:?}", db_path);
        tracing::info!("log_dir: {:?}", log_dir);

        app.handle().plugin(
            tauri_plugin_log::Builder::new()
                .targets([
                    tauri_plugin_log::Target::new(tauri_plugin_log::TargetKind::Folder {
                        path: log_dir,
                        file_name: None,
                    }),
                    #[cfg(debug_assertions)]
                    tauri_plugin_log::Target::new(tauri_plugin_log::TargetKind::Stdout),
                ])
                .level(tauri_plugin_log::log::LevelFilter::Warn)
                .level_for("acerola_p2p", tauri_plugin_log::log::LevelFilter::Debug)
                .level_for("acerola_lib", tauri_plugin_log::log::LevelFilter::Debug)
                .build(),
        )?;

        handle.manage(ReaderService::new());

        tauri::async_runtime::block_on(async move {
            setup_database(&handle, db_path).await;
            setup_network(&handle).await;
            setup_scopes_from_store(&handle).await;
        });

        Ok(())
    }

    fn resolve_paths(app: &tauri::App) -> (PathBuf, PathBuf) {
        let base = app.path().app_data_dir().unwrap();
        let logs = base.join("logs");

        std::fs::create_dir_all(&logs).unwrap();
        (base.join("acerola.db"), logs)
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    use pdfium_render::prelude::Pdfium;

    let pdfium_path = std::env::current_exe()
        .ok()
        .and_then(|exe| exe.parent().map(|p| p.to_path_buf()));

    let pdfium_bindings = if let Some(ref exe_dir) = pdfium_path {
        let resource_dir = exe_dir.join("_up_").join(".bin");
        if resource_dir.exists() {
            Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path(&resource_dir))
        } else {
            let local_bin = exe_dir.join(".bin");
            if local_bin.exists() {
                Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path(&local_bin))
            } else {
                Pdfium::bind_to_library(Pdfium::pdfium_platform_library_name_at_path(exe_dir))
            }
        }
    } else {
        Pdfium::bind_to_system_library()
    };

    let pdfium = Pdfium::new(
        pdfium_bindings
            .or_else(|_| Pdfium::bind_to_system_library())
            .expect("Failed to bind to Pdfium library"),
    );
    std::mem::forget(pdfium);

    let app_context = tauri::generate_context!();
    app_bootstrap::build().run(app_context).expect("Erro ao executar a aplicação Tauri");
}

pub mod system_cmd {
    #[tauri::command]
    pub fn get_package_family_name() -> String {
        #[cfg(target_os = "windows")]
        {
            use windows::ApplicationModel::Package;
            match Package::Current() {
                Ok(package) => match package.Id() {
                    Ok(id) => match id.FamilyName() {
                        Ok(name) => name.to_string(),
                        Err(_) => "Error retrieving Family Name".to_string(),
                    },
                    Err(_) => "Error retrieving Package ID".to_string(),
                },
                Err(_) => "No package identity".to_string(),
            }
        }
        #[cfg(not(target_os = "windows"))]
        {
            "Not running on Windows".to_string()
        }
    }

    #[tauri::command]
    pub fn open_filesystem_access_settings() {
        #[cfg(target_os = "windows")]
        {
            std::process::Command::new("cmd")
                .args(["/C", "start", "ms-settings:privacy-broadfilesystemaccess"])
                .spawn()
                .ok();
        }
    }
}
