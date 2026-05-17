mod cmd;
mod core;
mod data;
mod infra;

use cmd::features::library::{comic_scanner_cmd, select_folder_cmd};
use cmd::features::network as network_cmd;
use cmd::features::summary as comic_summary_cmd;
use tauri::Manager;

#[cfg(test)]
pub mod tests;

mod app_bootstrap {
    use crate::core::services::network::NetworkService;

    use super::*;
    use acerola_p2p::api::{
        guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
        identity::{DefaultDeviceInfoProvider, DeviceInfoProvider},
        transport::IrohTransportBuilder,
        AcerolaP2p,
    };
    use std::{path::PathBuf, sync::Arc};
    use tauri::Emitter;

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

        handle.manage(pool);
    }

    async fn setup_network(handle: &tauri::AppHandle) {
        let app = handle.clone();

        let emit: acerola_p2p::api::protocol::EventEmitter = Arc::new(move |event, data| {
            app.emit(event, data).ok();
        });

        let store = Arc::new(InMemoryTrustedStore::new());

        let transport = IrohTransportBuilder::default()
            .seed(*b"acerola-desktop-seed-v1-00000000")
            .relay("https://relay.acerola-comic.com")
            ;

        let device = DefaultDeviceInfoProvider::new("0.0.1-beta")
            .provide()
            .expect("Failed to read device info");

        let node = AcerolaP2p::builder(emit, transport, device)
            .guard(TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator())
            .build()
            .await
            .expect("Failed to start the p2p node");

        let service = NetworkService::new(Arc::new(node));
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
    app_bootstrap::build()
        .run(tauri::generate_context!())
        .expect("Error while running tauri application");
}
