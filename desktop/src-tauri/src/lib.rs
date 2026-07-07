#![allow(dead_code)]

mod cmd;
mod core;
mod data;
mod infra;

use cmd::features::{
    category::category_cmd,
    history as history_cmd,
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
    // INFO: Configura a localização da biblioteca PDFium antes de iniciar o app
    #[cfg(debug_assertions)]
    {
        // Em desenvolvimento, busca a pasta .bin relativa ao diretório do manifesto (Cargo.toml)
        let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap_or_else(|_| ".".to_string());
        let bin_path = std::path::Path::new(&manifest_dir).join(".bin");
        pdfium::set_library_location(bin_path.to_str().unwrap_or("."));
    }

    let app_context = tauri::generate_context!();

    #[cfg(not(debug_assertions))]
    {
        // Em produção, a DLL é empacotada como um recurso.
        // Como o set_library_location precisa ser chamado cedo, tentamos prever o caminho.
        // Geralmente, os recursos ficam em uma pasta específica relativa ao executável.
        if let Ok(exe_path) = std::env::current_exe() {
            if let Some(exe_dir) = exe_path.parent() {
                // O Tauri organiza os recursos em uma estrutura específica
                let resource_dir = exe_dir.join("_up_").join(".bin");
                if resource_dir.exists() {
                    pdfium::set_library_location(resource_dir.to_str().unwrap_or("."));
                } else {
                    let local_bin = exe_dir.join(".bin");
                    if local_bin.exists() {
                        pdfium::set_library_location(local_bin.to_str().unwrap_or("."));
                    } else {
                        // Fallback para a pasta do executável
                        pdfium::set_library_location(exe_dir.to_str().unwrap_or("."));
                    }
                }
            }
        }
    }

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
}
