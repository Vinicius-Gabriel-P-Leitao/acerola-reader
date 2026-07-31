use sqlx::{query_as, SqlitePool};

use crate::{
    data::{
        models::views::ComicSummaryView,
        repositories::{Entity, Repository},
    },
    infra::error::DbError,
};

/// Criteria para ordenação da biblioteca na Home.
#[derive(Debug, Clone, Copy, Default)]
pub enum SortCriteria {
    #[default]
    TitleAsc,
    TitleDesc,
    ChapterCountAsc,
    ChapterCountDesc,
}

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

    /// Busca todos os quadrinhos com ordenação específica.
    /// Suporta ordenação por título (ASC/DESC) e contagem de capítulos (ASC/DESC).
    pub async fn find_all_sorted(
        &self, criteria: SortCriteria,
    ) -> Result<Vec<ComicSummaryView>, DbError> {
        let table = ComicSummaryView::table_name();
        let cols = ComicSummaryView::columns().join(", ");

        let order_clause = match criteria {
            SortCriteria::TitleAsc => "ORDER BY COALESCE(metadata_title, folder_name) ASC",
            SortCriteria::TitleDesc => "ORDER BY COALESCE(metadata_title, folder_name) DESC",
            SortCriteria::ChapterCountAsc | SortCriteria::ChapterCountDesc => {
                // Para ordenação por contagem de capítulos, precisamos de uma subquery
                // porque a view não tem a contagem de capítulos diretamente.
                // Fazemos join com a contagem de chapters.
                return self.find_all_sorted_by_chapter_count(criteria).await;
            },
        };

        let sql = format!("SELECT {} FROM {} {}", cols, table, order_clause);

        let result = query_as::<_, ComicSummaryView>(&sql).fetch_all(&self.pool).await?;
        Ok(result)
    }

    /// Busca todos os quadrinhos ordenados por contagem de capítulos.
    async fn find_all_sorted_by_chapter_count(
        &self, criteria: SortCriteria,
    ) -> Result<Vec<ComicSummaryView>, DbError> {
        let table = ComicSummaryView::table_name();
        let cols = ComicSummaryView::columns().join(", ");

        let order_direction = match criteria {
            SortCriteria::ChapterCountAsc => "ASC",
            SortCriteria::ChapterCountDesc => "DESC",
            _ => "ASC",
        };

        // Join com a contagem de chapters para ordenar.
        // LEFT JOIN garante que quadrinhos sem capítulos apareçam (count = 0).
        let sql = format!(
            "SELECT {} FROM {} v LEFT JOIN (SELECT comic_directory_fk, COUNT(*) as chapter_count FROM chapter_archive GROUP BY comic_directory_fk) c ON v.directory_id = c.comic_directory_fk ORDER BY COALESCE(c.chapter_count, 0) {}",
            cols, table, order_direction
        );

        let result = query_as::<_, ComicSummaryView>(&sql).fetch_all(&self.pool).await?;
        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db::setup_test_db;

    async fn setup() -> (SqlitePool, HomeRepository) {
        let pool = setup_test_db().await;
        let repo = HomeRepository::new(pool.clone());
        (pool, repo)
    }

    async fn insert_comic_with_chapters(
        pool: &SqlitePool, id: i64, name: &str, title: &str, chapter_count: i64,
    ) {
        sqlx::query(
            "INSERT INTO comic_directory (id, name, path, last_modified) VALUES (?, ?, ?, 0)",
        )
        .bind(id)
        .bind(name)
        .bind(format!("/mangas/{}", name.to_lowercase()))
        .execute(pool)
        .await
        .unwrap();

        sqlx::query("INSERT INTO comic_metadata (id, comic_directory_fk, title, description, status) VALUES (?, ?, ?, '', '')")
            .bind(id)
            .bind(id)
            .bind(title)
            .execute(pool)
            .await
            .unwrap();

        for i in 0..chapter_count {
            sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (?, ?, ?, ?, 0, ?, 0)")
                .bind(format!("{}{}", id, i))
                .bind(format!("Capítulo {}", i))
                .bind(format!("/mangas/{}/cap{}", name.to_lowercase(), i))
                .bind(format!("{:03}", i))
                .bind(id)
                .execute(pool)
                .await
                .unwrap();
        }
    }

    #[tokio::test]
    async fn teste_busca_por_titulo_lower_e_upper() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO comic_directory (id, name, path, last_modified) VALUES (1, 'One Piece', '/mangas/one piece', 0)")
            .execute(&pool)
            .await
            .unwrap();
        sqlx::query("INSERT INTO comic_metadata (id, comic_directory_fk, title, description, status) VALUES (1, 1, 'One Piece - Piratas', 'desc', 'status')")
            .execute(&pool)
            .await
            .unwrap();

        sqlx::query("INSERT INTO comic_directory (id, name, path, last_modified) VALUES (2, 'NARUTO', '/mangas/naruto', 0)")
            .execute(&pool)
            .await
            .unwrap();

        let results_upper = repo.search_by_title("PIECE").await.unwrap();
        assert_eq!(results_upper.len(), 1);
        assert_eq!(results_upper[0].folder_name, "One Piece");

        let results_lower = repo.search_by_title("naruto").await.unwrap();
        assert_eq!(results_lower.len(), 1);
        assert_eq!(results_lower[0].folder_name, "NARUTO");

        let results_empty = repo.search_by_title("Dragon Ball").await.unwrap();
        assert_eq!(results_empty.len(), 0);
    }

    #[tokio::test]
    async fn teste_ordenacao_por_titulo_asc() {
        let (pool, repo) = setup().await;

        insert_comic_with_chapters(&pool, 1, "Zatch Bell", "Zatch Bell", 1).await;
        insert_comic_with_chapters(&pool, 2, "Attack on Titan", "Attack on Titan", 1).await;
        insert_comic_with_chapters(&pool, 3, "Berserk", "Berserk", 1).await;

        let result = repo.find_all_sorted(SortCriteria::TitleAsc).await.unwrap();
        assert_eq!(result.len(), 3);
        assert_eq!(result[0].metadata_title, Some("Attack on Titan".to_string()));
        assert_eq!(result[1].metadata_title, Some("Berserk".to_string()));
        assert_eq!(result[2].metadata_title, Some("Zatch Bell".to_string()));
    }

    #[tokio::test]
    async fn teste_ordenacao_por_titulo_desc() {
        let (pool, repo) = setup().await;

        insert_comic_with_chapters(&pool, 1, "Zatch Bell", "Zatch Bell", 1).await;
        insert_comic_with_chapters(&pool, 2, "Attack on Titan", "Attack on Titan", 1).await;
        insert_comic_with_chapters(&pool, 3, "Berserk", "Berserk", 1).await;

        let result = repo.find_all_sorted(SortCriteria::TitleDesc).await.unwrap();
        assert_eq!(result.len(), 3);
        assert_eq!(result[0].metadata_title, Some("Zatch Bell".to_string()));
        assert_eq!(result[1].metadata_title, Some("Berserk".to_string()));
        assert_eq!(result[2].metadata_title, Some("Attack on Titan".to_string()));
    }

    #[tokio::test]
    async fn teste_ordenacao_por_contagem_capitulos_asc() {
        let (pool, repo) = setup().await;

        insert_comic_with_chapters(&pool, 1, "Manga A", "Manga A", 5).await;
        insert_comic_with_chapters(&pool, 2, "Manga B", "Manga B", 2).await;
        insert_comic_with_chapters(&pool, 3, "Manga C", "Manga C", 10).await;

        let result = repo.find_all_sorted(SortCriteria::ChapterCountAsc).await.unwrap();
        assert_eq!(result.len(), 3);
        // Ordenado por contagem: B (2), A (5), C (10)
        assert_eq!(result[0].folder_name, "Manga B");
        assert_eq!(result[1].folder_name, "Manga A");
        assert_eq!(result[2].folder_name, "Manga C");
    }

    #[tokio::test]
    async fn teste_ordenacao_por_contagem_capitulos_desc() {
        let (pool, repo) = setup().await;

        insert_comic_with_chapters(&pool, 1, "Manga A", "Manga A", 5).await;
        insert_comic_with_chapters(&pool, 2, "Manga B", "Manga B", 2).await;
        insert_comic_with_chapters(&pool, 3, "Manga C", "Manga C", 10).await;

        let result = repo.find_all_sorted(SortCriteria::ChapterCountDesc).await.unwrap();
        assert_eq!(result.len(), 3);
        // Ordenado por contagem: C (10), A (5), B (2)
        assert_eq!(result[0].folder_name, "Manga C");
        assert_eq!(result[1].folder_name, "Manga A");
        assert_eq!(result[2].folder_name, "Manga B");
    }
}
