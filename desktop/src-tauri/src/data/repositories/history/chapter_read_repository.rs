use sqlx::{query, Pool, Sqlite};

use crate::{data::models::history::chapter_read::ChapterRead, infra::error::DbError};

#[derive(Clone)]
pub struct ChapterReadRepository {
    pool: Pool<Sqlite>,
}

impl ChapterReadRepository {
    pub fn new(pool: Pool<Sqlite>) -> Self {
        Self { pool }
    }

    pub async fn insert(&self, chapter_read: &ChapterRead) -> Result<ChapterRead, DbError> {
        let sql = r#"
            INSERT INTO chapter_read (comic_directory_id, chapter_archive_id, created_at)
            VALUES (?, ?, ?)
            ON CONFLICT(comic_directory_id, chapter_archive_id) DO NOTHING
            RETURNING *
        "#;

        let row_opt = query(sql)
            .bind(chapter_read.comic_directory_id)
            .bind(chapter_read.chapter_archive_id)
            .bind(chapter_read.created_at)
            .fetch_optional(&self.pool)
            .await?;

        if let Some(row) = row_opt {
            Ok(sqlx::FromRow::from_row(&row)?)
        } else {
            // Se já existia e não inseriu nada, retornamos o que foi passado, 
            // assumindo que os dados não mudaram (é apenas criado).
            Ok(chapter_read.clone())
        }
    }

    pub async fn get_read_chapters(&self, comic_directory_id: i64) -> Result<Vec<i64>, DbError> {
        let sql = "SELECT chapter_archive_id FROM chapter_read WHERE comic_directory_id = ?";
        let rows = query(sql)
            .bind(comic_directory_id)
            .fetch_all(&self.pool)
            .await?;
            
        Ok(rows.into_iter().map(|row| sqlx::Row::get(&row, "chapter_archive_id")).collect())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db::setup_test_db_with_comic;

    async fn setup() -> (Pool<Sqlite>, ChapterReadRepository) {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterReadRepository::new(pool.clone());
        (pool, repo)
    }

    #[tokio::test]
    async fn teste_insert_chapter_read() {
        let (pool, repo) = setup().await;
        
        query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)").execute(&pool).await.unwrap();

        let chapter_read = ChapterRead {
            comic_directory_id: 1,
            chapter_archive_id: 1,
            created_at: 1000,
        };

        let result = repo.insert(&chapter_read).await.unwrap();
        assert_eq!(result.comic_directory_id, 1);
        
        // Inserir novamente não deve dar erro (DO NOTHING)
        let result2 = repo.insert(&chapter_read).await.unwrap();
        assert_eq!(result2.comic_directory_id, 1);
    }
}
