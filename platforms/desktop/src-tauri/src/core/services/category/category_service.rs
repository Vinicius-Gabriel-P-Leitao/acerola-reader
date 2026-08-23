use sqlx::SqlitePool;

use crate::{
    data::{
        models::category::{category::Category, comic_category::ComicCategory},
        repositories::category::category_repo::CategoryRepository,
    },
    infra::error::DbError,
};

/// Serviço responsável pelas regras de negócio de categorias (marcadores).
pub struct CategoryService {
    repo: CategoryRepository,
}

impl CategoryService {
    /// Inicializa o serviço de categorias.
    pub fn new(pool: SqlitePool) -> Self {
        Self { repo: CategoryRepository::new(pool) }
    }

    /// Cria uma nova categoria (marcador) com nome e cor.
    pub async fn create_category(&self, name: String, color: i64) -> Result<Category, DbError> {
        let category = Category { id: None, name, color };
        self.repo.base.insert(&category).await
    }

    /// Retorna todas as categorias (marcadores) cadastradas.
    pub async fn get_categories(&self) -> Result<Vec<Category>, DbError> {
        self.repo.base.find_all().await
    }

    /// Deleta uma categoria pelo seu ID.
    pub async fn delete_category(&self, id: i64) -> Result<(), DbError> {
        self.repo.base.delete(id).await
    }

    /// Atribui uma categoria a um quadrinho, garantindo que seja única por quadrinho.
    pub async fn assign_category_to_comic(
        &self, comic_id: i64, category_id: i64,
    ) -> Result<ComicCategory, DbError> {
        // Garante que qualquer associação anterior seja removida
        self.repo.remove_category_from_comic(comic_id).await?;

        let assignment = ComicCategory { id: None, comic_directory_fk: comic_id, category_id };
        self.repo.comic_category_base.insert(&assignment).await
    }

    /// Remove o marcador de um quadrinho.
    pub async fn remove_category_from_comic(&self, comic_id: i64) -> Result<(), DbError> {
        self.repo.remove_category_from_comic(comic_id).await
    }

    /// Busca a categoria associada a um quadrinho específico.
    pub async fn get_comic_category(&self, comic_id: i64) -> Result<Option<Category>, DbError> {
        self.repo.get_comic_category(comic_id).await
    }

    pub async fn get_all_comic_categories(&self) -> Result<Vec<ComicCategory>, DbError> {
        self.repo.comic_category_base.find_all().await
    }
}
