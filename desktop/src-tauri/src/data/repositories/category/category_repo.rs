use sqlx::{query_as, SqlitePool};

use crate::{
    data::models::category::{category::Category, comic_category::ComicCategory},
    infra::error::DbError,
};

/// Repositório para gerenciar as categorias (marcadores) e sua relação com quadrinhos.
pub struct CategoryRepository {
    pool: SqlitePool,
}

impl CategoryRepository {
    /// Instancia o repositório.
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Remove a associação de um quadrinho com sua categoria atual (se existir).
    pub async fn remove_category_from_comic(&self, comic_id: i64) -> Result<(), DbError> {
        sqlx::query("DELETE FROM manga_category WHERE comic_directory_fk = ?")
            .bind(comic_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }

    /// Busca a categoria associada a um quadrinho, se houver.
    pub async fn get_comic_category(&self, comic_id: i64) -> Result<Option<Category>, DbError> {
        let result = query_as::<_, Category>(
            "SELECT c.* FROM category c 
             JOIN manga_category mc ON c.id = mc.category_id 
             WHERE mc.comic_directory_fk = ?",
        )
        .bind(comic_id)
        .fetch_optional(&self.pool)
        .await?;
        Ok(result)
    }
}
