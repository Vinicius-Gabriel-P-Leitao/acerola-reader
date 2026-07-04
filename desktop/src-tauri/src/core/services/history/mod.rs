use serde::Serialize;
use sqlx::{query_as, Pool, Sqlite};
use std::time::{SystemTime, UNIX_EPOCH};

use crate::{
    data::{
        models::history::{chapter_read::ChapterRead, reading_history::ReadingHistory},
        repositories::history::{
            chapter_read_repository::ChapterReadRepository,
            reading_history_repository::ReadingHistoryRepository,
        },
    },
    infra::error::DbError,
};

fn i64_to_string<S>(val: &i64, serializer: S) -> Result<S::Ok, S::Error>
where
    S: serde::Serializer,
{
    serializer.serialize_str(&val.to_string())
}

#[derive(Debug, Serialize, sqlx::FromRow)]
#[serde(rename_all = "camelCase")]
pub struct ReadingHistoryView {
    #[serde(serialize_with = "i64_to_string")]
    pub comic_directory_id: i64,
    #[serde(serialize_with = "i64_to_string")]
    pub chapter_archive_id: i64,
    pub last_page: i64,
    pub is_completed: bool,
    pub updated_at: i64,
    pub comic_name: String,
    pub comic_cover: Option<String>,
    pub chapter_name: String,
    pub folder_name: String,
    pub chapter_path: String,
    pub chapter_sort: String,
    pub is_special: bool,
    pub last_modified: i64,
}

#[derive(Clone)]
pub struct HistoryService {
    reading_repo: ReadingHistoryRepository,
    chapter_repo: ChapterReadRepository,
    pool: Pool<Sqlite>,
}

impl HistoryService {
    pub fn new(pool: Pool<Sqlite>) -> Self {
        Self {
            reading_repo: ReadingHistoryRepository::new(pool.clone()),
            chapter_repo: ChapterReadRepository::new(pool.clone()),
            pool,
        }
    }

    pub async fn update_reading_history(
        &self, comic_directory_id: i64, chapter_archive_id: i64, last_page: i64, is_completed: bool,
    ) -> Result<ReadingHistory, DbError> {
        let now = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs() as i64;
        
        let history = ReadingHistory {
            comic_directory_id,
            chapter_archive_id,
            last_page,
            is_completed,
            updated_at: now,
        };

        let result = self.reading_repo.upsert(&history).await?;

        if is_completed {
            let chapter_read = ChapterRead {
                comic_directory_id,
                chapter_archive_id,
                created_at: now,
            };
            self.chapter_repo.insert(&chapter_read).await?;
        }

        Ok(result)
    }

    pub async fn get_reading_history(&self, comic_directory_id: i64) -> Result<Option<ReadingHistory>, DbError> {
        self.reading_repo.find_by_comic_id(comic_directory_id).await
    }

    pub async fn get_comic_history_view(&self, comic_directory_id: i64) -> Result<Option<ReadingHistoryView>, DbError> {
        let sql = r#"
            SELECT 
                rh.comic_directory_id,
                rh.chapter_archive_id,
                rh.last_page,
                rh.is_completed,
                rh.updated_at,
                c.name as comic_name,
                c.cover as comic_cover,
                ca.chapter as chapter_name,
                c.name as folder_name,
                ca.path as chapter_path,
                ca.chapter_sort,
                ca.is_special,
                ca.last_modified
            FROM reading_history rh
            JOIN comic_directory c ON rh.comic_directory_id = c.id
            JOIN chapter_archive ca ON rh.chapter_archive_id = ca.id
            WHERE rh.comic_directory_id = ?
        "#;
        
        let result = query_as::<_, ReadingHistoryView>(sql)
            .bind(comic_directory_id)
            .fetch_optional(&self.pool)
            .await?;
            
        Ok(result)
    }

    pub async fn get_full_history(&self) -> Result<Vec<ReadingHistoryView>, DbError> {
        let sql = r#"
            SELECT 
                rh.comic_directory_id,
                rh.chapter_archive_id,
                rh.last_page,
                rh.is_completed,
                rh.updated_at,
                c.name as comic_name,
                c.cover as comic_cover,
                ca.chapter as chapter_name,
                c.name as folder_name,
                ca.path as chapter_path,
                ca.chapter_sort,
                ca.is_special,
                ca.last_modified
            FROM reading_history rh
            JOIN comic_directory c ON rh.comic_directory_id = c.id
            JOIN chapter_archive ca ON rh.chapter_archive_id = ca.id
            ORDER BY rh.updated_at DESC
        "#;
        
        let result = query_as::<_, ReadingHistoryView>(sql)
            .fetch_all(&self.pool)
            .await?;
            
        Ok(result)
    }
    
    pub async fn clear_history(&self) -> Result<(), DbError> {
        sqlx::query("DELETE FROM reading_history").execute(&self.pool).await?;
        Ok(())
    }

    pub async fn get_read_chapters(&self, comic_directory_id: i64) -> Result<Vec<i64>, DbError> {
        self.chapter_repo.get_read_chapters(comic_directory_id).await
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db::setup_test_db_with_comic;
    use sqlx::query;

    async fn setup() -> (Pool<Sqlite>, HistoryService) {
        let pool = setup_test_db_with_comic().await;
        let service = HistoryService::new(pool.clone());
        (pool, service)
    }

    #[tokio::test]
    async fn teste_atualiza_leitura_de_quadrinho() {
        // Atualiza o histórico de leitura com sucesso
        let (pool, service) = setup().await;
        
        // Insere capitulo basico no banco para satisfazer chaves estrangeiras
        query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)").execute(&pool).await.unwrap();

        let result = service.update_reading_history(1, 1, 10, false).await.unwrap();
        
        assert_eq!(result.comic_directory_id, 1);
        assert_eq!(result.chapter_archive_id, 1);
        assert_eq!(result.last_page, 10);
        assert_eq!(result.is_completed, false);
    }

    #[tokio::test]
    async fn teste_limpa_historico_do_banco() {
        // Limpa o histórico corretamente
        let (pool, service) = setup().await;
        
        // Insere capitulo basico no banco para satisfazer chaves estrangeiras
        query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)").execute(&pool).await.unwrap();

        service.update_reading_history(1, 1, 10, false).await.unwrap();
        
        let history_before = service.get_full_history().await.unwrap();
        assert_eq!(history_before.len(), 1);
        
        service.clear_history().await.unwrap();
        
        let history_after = service.get_full_history().await.unwrap();
        assert_eq!(history_after.len(), 0);
    }
}
