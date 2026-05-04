use crate::{
    cmd::events::{shared::ErrorPayload, summary::ComicSummaryPayload},
    core::services::summary::HomeService,
};

use sqlx::SqlitePool;
use tauri::{AppHandle, Emitter, State};

#[tauri::command]
pub async fn get_comic_summary(app: AppHandle, pool: State<'_, SqlitePool>) -> Result<(), String> {
    let pool = pool.inner().clone();

    tokio::spawn(async move {
        let service = HomeService::new(pool);

        match service.get_all().await {
            Ok(comics) => app.emit("home:data", ComicSummaryPayload::from(comics)).unwrap(),
            Err(err) => app.emit("home:error", ErrorPayload::from(&err)).unwrap(),
        }
    });

    Ok(())
}
