use sqlx::{query_as, SqlitePool};

use crate::{
    data::{
        models::views::ComicSummaryView,
        repositories::{Entity, Repository},
    },
    infra::error::DbError,
};

#[derive(Clone)]
pub struct HomeRepository {
    pub base: Repository<ComicSummaryView>,
    pool: SqlitePool,
}

impl HomeRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool.clone()), pool }
    }

    pub async fn search_by_title(&self, query_str: &str) -> Result<Vec<ComicSummaryView>, DbError> {
        let table = ComicSummaryView::table_name();
        let cols = ComicSummaryView::columns().join(", ");
        let search_pattern = format!("%{}%", query_str.to_lowercase());

        let sql = format!(
            "SELECT {} FROM {} WHERE LOWER(folder_name) LIKE ? OR LOWER(metadata_title) LIKE ?",
            cols, table
        );

        let result = query_as::<_, ComicSummaryView>(&sql)
            .bind(&search_pattern)
            .bind(&search_pattern)
            .fetch_all(&self.pool)
            .await?;
        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db::setup_test_db;

    async fn setup() -> (SqlitePool, HomeRepository) {
        let pool = setup_test_db().await;
        // The view comic_summary_view depends on comic_directory and comic_metadata.
        // We will create mock tables for them just for this test, as setup_test_db might only create the db.
        // Actually, setup_test_db should run the migrations. Let's assume migrations are run.
        let repo = HomeRepository::new(pool.clone());
        (pool, repo)
    }

    #[tokio::test]
    async fn teste_busca_por_titulo_lower_e_upper() {
        let (pool, repo) = setup().await;

        // Inserir dados nas tabelas base
        sqlx::query("INSERT INTO comic_directory (id, name, path, last_modified) VALUES (1, 'One Piece', '/mangas/one piece', 0)")
            .execute(&pool)
            .await
            .unwrap();
        sqlx::query("INSERT INTO comic_metadata (id, comic_directory_fk, title, description, romanji, status) VALUES (1, 1, 'One Piece - Piratas', 'desc', 'romanji', 'status')")
            .execute(&pool)
            .await
            .unwrap();

        sqlx::query("INSERT INTO comic_directory (id, name, path, last_modified) VALUES (2, 'NARUTO', '/mangas/naruto', 0)")
            .execute(&pool)
            .await
            .unwrap();

        // Buscar em uppercase
        let results_upper = repo.search_by_title("PIECE").await.unwrap();
        assert_eq!(results_upper.len(), 1);
        assert_eq!(results_upper[0].folder_name, "One Piece");

        // Buscar em lowercase
        let results_lower = repo.search_by_title("naruto").await.unwrap();
        assert_eq!(results_lower.len(), 1);
        assert_eq!(results_lower[0].folder_name, "NARUTO");

        // Buscar sem resultados
        let results_empty = repo.search_by_title("Dragon Ball").await.unwrap();
        assert_eq!(results_empty.len(), 0);
    }
}
