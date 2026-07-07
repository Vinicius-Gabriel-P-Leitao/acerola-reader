use sqlx::SqlitePool;

use crate::{
    data::{
        models::category::{category::Category, comic_category::ComicCategory},
        repositories::Repository,
    },
    infra::error::DbError,
};

/// Repositório para gerenciar as categorias (marcadores) e sua relação com quadrinhos.
pub struct CategoryRepository {
    pub comic_category_base: Repository<ComicCategory>,
    pub base: Repository<Category>,
    pool: SqlitePool,
}

impl CategoryRepository {
    /// Instancia o repositório.
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            base: Repository::new(pool.clone()),
            comic_category_base: Repository::new(pool.clone()),
            pool,
        }
    }

    /// Remove a associação de um quadrinho com sua categoria atual (se existir).
    pub async fn remove_category_from_comic(&self, comic_id: i64) -> Result<(), DbError> {
        sqlx::query("DELETE FROM comic_category WHERE comic_directory_fk = ?")
            .bind(comic_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }

    /// Busca a categoria associada a um quadrinho, se houver.
    pub async fn get_comic_category(&self, comic_id: i64) -> Result<Option<Category>, DbError> {
        let result = sqlx::query_as::<_, Category>(
            "SELECT c.id, c.name, c.color FROM category c
             INNER JOIN comic_category cc ON c.id = cc.category_id
             WHERE cc.comic_directory_fk = ?",
        )
        .bind(comic_id)
        .fetch_optional(&self.pool)
        .await?;

        Ok(result)
    }
}
