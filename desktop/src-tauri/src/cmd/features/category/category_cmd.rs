use sqlx::SqlitePool;
use tauri::State;

use crate::{
    cmd::events::shared::ErrorPayload,
    core::services::category::category_service::CategoryService,
    data::models::category::{category::Category, comic_category::ComicCategory},
};

/// Comando Tauri para criar uma nova categoria (marcador).
#[tauri::command]
pub async fn create_category(
    name: String, color: i64, pool: State<'_, SqlitePool>,
) -> Result<Category, ErrorPayload> {
    let service = CategoryService::new(pool.inner().clone());
    service.create_category(name, color).await.map_err(|error| ErrorPayload::from(&error.into()))
}

/// Comando Tauri para listar todas as categorias (marcadores).
#[tauri::command]
pub async fn get_categories(pool: State<'_, SqlitePool>) -> Result<Vec<Category>, ErrorPayload> {
    let service = CategoryService::new(pool.inner().clone());
    service.get_categories().await.map_err(|error| ErrorPayload::from(&error.into()))
}

/// Comando Tauri para remover uma categoria.
#[tauri::command]
pub async fn delete_category(id: i64, pool: State<'_, SqlitePool>) -> Result<(), ErrorPayload> {
    let service = CategoryService::new(pool.inner().clone());
    service.delete_category(id).await.map_err(|error| ErrorPayload::from(&error.into()))
}

/// Comando Tauri para vincular uma categoria a um quadrinho.
#[tauri::command]
pub async fn assign_category_to_comic(
    comic_id: i64, category_id: i64, pool: State<'_, SqlitePool>,
) -> Result<ComicCategory, ErrorPayload> {
    let service = CategoryService::new(pool.inner().clone());
    service
        .assign_category_to_comic(comic_id, category_id)
        .await
        .map_err(|error| ErrorPayload::from(&error.into()))
}

/// Comando Tauri para remover a categoria atual de um quadrinho.
#[tauri::command]
pub async fn remove_category_from_comic(
    comic_id: i64, pool: State<'_, SqlitePool>,
) -> Result<(), ErrorPayload> {
    let service = CategoryService::new(pool.inner().clone());
    service.remove_category_from_comic(comic_id).await.map_err(|error| ErrorPayload::from(&error.into()))
}

/// Comando Tauri para buscar a categoria associada a um quadrinho.
#[tauri::command]
pub async fn get_comic_category(
    comic_id: i64, pool: State<'_, SqlitePool>,
) -> Result<Option<Category>, ErrorPayload> {
    let service = CategoryService::new(pool.inner().clone());
    service.get_comic_category(comic_id).await.map_err(|error| ErrorPayload::from(&error.into()))
}
