mod cmd;
mod core;
mod data;
mod infra;

use cmd::features::library::{comic_scanner_cmd, select_folder_cmd};
use cmd::features::network::network_cmd;
use cmd::features::summary::comic_summary_cmd;
use tauri::Manager;

#[cfg(test)]
pub mod tests;

mod app_bootstrap {
    use crate::{
        core::{
            connection::p2p::{handlers, network_manager},
            services::network::network_service::NetworkService,
        },
        data::remote::p2p::{iroh_transport, open_guard::open_guard},
    };

    use super::*;
    use std::{path::PathBuf, sync::Arc};

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
            select_folder_cmd::select_folder,
            comic_summary_cmd::get_comic_summary,
            comic_scanner_cmd::refresh_library,
            comic_scanner_cmd::rebuild_library,
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
                .add_migrations("sqlite:acerola.db", crate::infra::db::migrations::get_migrations())
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
        let transport = Arc::new(iroh_transport::IrohTransport::new().await.unwrap());
        let transport_clone = Arc::clone(&transport);

        let (mut manager, command_tx, state) = network_manager::NetworkManager::new(
            transport,
            Box::new(|ctx| Box::pin(open_guard(ctx))),
        );

        manager.register(b"acerola/rpc", Arc::new(handlers::rpc::RpcHandler::new()));
        tokio::spawn(manager.run());

        let service = NetworkService::new(state, transport_clone, command_tx);
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
                .level({
                    #[cfg(debug_assertions)]
                    {
                        tauri_plugin_log::log::LevelFilter::Debug
                    }

                    #[cfg(not(debug_assertions))]
                    {
                        tauri_plugin_log::log::LevelFilter::Info
                    }
                })
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
