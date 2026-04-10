mod commands;
mod core;
mod data;
mod infra;

use commands::features::library::{ comic_scanner_cmd, select_folder_cmd };
use tauri::Manager;

#[cfg(test)]
pub mod tests;

mod app_bootstrap {
    use super::*;
    use std::path::PathBuf;

    pub fn build() -> tauri::Builder<tauri::Wry> {
        let builder = tauri::Builder::default();
        let builder = setup_opener(builder);
        let builder = setup_dialog(builder);
        let builder = setup_store(builder);
        let builder = setup_sql(builder);
        let builder = setup_fs(builder);

        // INFO: Commands que serão chamados via invoke
        // prettier-ignore
        builder.setup(setup_runtime).invoke_handler(
                tauri::generate_handler![
                    select_folder_cmd::select_folder,
                    comic_scanner_cmd::comic_scanner
                ]
        )
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
            // prettier-ignore
            tauri_plugin_sql::Builder::new()
                .add_migrations("sqlite:acerola.db", crate::infra::db::migrations::get_migrations()).build()
        )
    }

    fn setup_runtime(app: &mut tauri::App) -> Result<(), Box<dyn std::error::Error>> {
        let handle: tauri::AppHandle = app.handle().clone();
        let (db_path, log_dir) = resolve_paths(app);

        app.handle().plugin(
            // prettier-ignore
            tauri_plugin_log::Builder::new().target(
                tauri_plugin_log::Target::new(tauri_plugin_log::TargetKind::Folder {
                    path: log_dir,
                    file_name: None,
                })
            ).level(tauri_plugin_log::log::LevelFilter::Info).build()
        )?;

        tauri::async_runtime::block_on(async move {
            // prettier-ignore
            let pool = sqlx::SqlitePool::connect(&format!("sqlite:{}?mode=rwc", db_path.to_string_lossy())).await.unwrap();
            handle.manage(pool);
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
    // prettier-ignore
    app_bootstrap::build().run(tauri::generate_context!()).expect("Error while running tauri application");
}
