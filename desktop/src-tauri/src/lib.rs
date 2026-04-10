mod data;
mod core;
mod infra;
mod commands;

use tauri::Manager;
use commands::features::library::{ select_folder_cmd, comic_scanner_cmd };

#[cfg(test)]
pub mod tests;

fn setup_opener(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
    builder.plugin(tauri_plugin_opener::init())
}

fn setup_dialog(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
    builder.plugin(tauri_plugin_dialog::init())
}

fn setup_store(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
    builder.plugin(tauri_plugin_store::Builder::new().build())
}

fn setup_fs(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
    builder.plugin(tauri_plugin_fs::init())
}

fn setup_sql(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
    builder.plugin(
        // prettier-ignore
        tauri_plugin_sql::Builder::new()
            .add_migrations("sqlite:acerola.db", crate::infra::db::migrations::get_migrations())
            .build()
    )
}

fn configure_all_plugins(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
    let builder = setup_opener(builder);
    let builder = setup_dialog(builder);
    let builder = setup_store(builder);
    let builder = setup_fs(builder);
    setup_sql(builder)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let builder = tauri::Builder::default();
    let builder = configure_all_plugins(builder);

    builder
        .setup(|app| {
            let handle = app.handle().clone();
            // prettier-ignore
            let db_path = app.path().app_data_dir().expect("Failed to resolve app_data_dir").join("acerola.db");

            tauri::async_runtime::block_on(async move {
                let pool = sqlx::SqlitePool
                    ::connect(&format!("sqlite:{}?mode=rwc", db_path.to_string_lossy())).await
                    .expect("Failed to connect to the database.");

                handle.manage(pool);
            });
            Ok(())
        })
        .invoke_handler(
            tauri::generate_handler![
                select_folder_cmd::select_folder,
                comic_scanner_cmd::comic_scanner
            ]
        )
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
