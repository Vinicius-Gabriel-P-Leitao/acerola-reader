use sqlx::{query, query_as, Pool, Sqlite};

use crate::{data::models::history::reading_history::ReadingHistory, infra::error::DbError};

#[derive(Clone)]
pub struct ReadingHistoryRepository {
    pool: Pool<Sqlite>,
}

impl ReadingHistoryRepository {
    pub fn new(pool: Pool<Sqlite>) -> Self {
        Self { pool }
    }

    pub async fn upsert(&self, history: &ReadingHistory) -> Result<ReadingHistory, DbError> {
        let sql = r#"
            INSERT INTO reading_history (comic_directory_id, chapter_archive_id, last_page, is_completed, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(comic_directory_id) DO UPDATE SET
                chapter_archive_id = excluded.chapter_archive_id,
                last_page = excluded.last_page,
                is_completed = excluded.is_completed,
                updated_at = excluded.updated_at
            RETURNING *
        "#;

        let row = query(sql)
            .bind(history.comic_directory_id)
            .bind(history.chapter_archive_id)
            .bind(history.last_page)
            .bind(history.is_completed)
            .bind(history.updated_at)
            .fetch_one(&self.pool)
            .await?;

        Ok(sqlx::FromRow::from_row(&row)?)
    }

    pub async fn find_by_comic_id(&self, comic_directory_id: i64) -> Result<Option<ReadingHistory>, DbError> {
        let sql = "SELECT * FROM reading_history WHERE comic_directory_id = ?";
        let result = query_as::<_, ReadingHistory>(sql)
            .bind(comic_directory_id)
            .fetch_optional(&self.pool)
            .await?;
        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db::setup_test_db_with_comic;

    async fn setup() -> (Pool<Sqlite>, ReadingHistoryRepository) {
        let pool = setup_test_db_with_comic().await;
        // The migrations should run during setup_test_db
        let repo = ReadingHistoryRepository::new(pool.clone());
        (pool, repo)
    }

    #[tokio::test]
    async fn teste_upsert_insert() {
        let (pool, repo) = setup().await;
        
        // We need to insert comic and chapter to satisfy foreign keys
        query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)").execute(&pool).await.unwrap();

        let history = ReadingHistory {
            comic_directory_id: 1,
            chapter_archive_id: 1,
            last_page: 5,
            is_completed: false,
            updated_at: 1000,
        };

        let result = repo.upsert(&history).await.unwrap();
        assert_eq!(result.comic_directory_id, 1);
        assert_eq!(result.last_page, 5);
    }

    #[tokio::test]
    async fn teste_upsert_update() {
        let (pool, repo) = setup().await;
        
        query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)").execute(&pool).await.unwrap();
        query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (2, '2', 'path', '2', 0, 1, 0)").execute(&pool).await.unwrap();

        let history1 = ReadingHistory {
            comic_directory_id: 1,
            chapter_archive_id: 1,
            last_page: 5,
            is_completed: false,
            updated_at: 1000,
        };
        repo.upsert(&history1).await.unwrap();

        let history2 = ReadingHistory {
            comic_directory_id: 1,
            chapter_archive_id: 2,
            last_page: 2,
            is_completed: true,
            updated_at: 2000,
        };
        let result = repo.upsert(&history2).await.unwrap();

        assert_eq!(result.chapter_archive_id, 2);
        assert_eq!(result.last_page, 2);
        assert_eq!(result.is_completed, true);
    }
}
