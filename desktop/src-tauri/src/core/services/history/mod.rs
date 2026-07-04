use std::time::{SystemTime, UNIX_EPOCH};

use sqlx::SqlitePool;

use crate::{
    cmd::events::history::ReadingHistoryPayload,
    data::{
        models::history::{chapter_read::ChapterRead, reading_history::ReadingHistory},
        repositories::history::{
            chapter_read_repo::ChapterReadRepository,
            reading_history_repo::ReadingHistoryRepository,
        },
    },
    infra::error::DbError,
};

#[derive(Clone)]
pub struct HistoryService {
    reading_repo: ReadingHistoryRepository,
    chapter_repo: ChapterReadRepository,
}

impl HistoryService {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            reading_repo: ReadingHistoryRepository::new(pool.clone()),
            chapter_repo: ChapterReadRepository::new(pool),
        }
    }

    /// Atualiza o progresso de leitura de um quadrinho. Se `is_completed`,
    /// registra também o capítulo como lido na tabela `chapter_read`.
    pub async fn update_progress(
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
            let chapter_read_entry =
                ChapterRead { comic_directory_id, chapter_archive_id, created_at: now };
            self.chapter_repo.insert_or_ignore(&chapter_read_entry).await?;
        }

        Ok(result)
    }

    /// Retorna o payload enriquecido de leitura de um quadrinho.
    pub async fn find_by_comic(
        &self, comic_directory_id: i64,
    ) -> Result<Option<ReadingHistoryPayload>, DbError> {
        self.reading_repo.find_payload_by_comic_id(comic_directory_id).await
    }

    /// Retorna todo o histórico enriquecido, ordenado pelo mais recente.
    pub async fn find_all(&self) -> Result<Vec<ReadingHistoryPayload>, DbError> {
        self.reading_repo.find_all_payload_ordered().await
    }

    /// Apaga todo o histórico de leitura.
    pub async fn clear(&self) -> Result<(), DbError> {
        self.reading_repo.delete_all().await
    }

    /// Retorna os IDs dos capítulos lidos de um quadrinho.
    pub async fn find_read_chapters(&self, comic_directory_id: i64) -> Result<Vec<i64>, DbError> {
        self.chapter_repo.find_ids_by_comic(comic_directory_id).await
    }
}

#[cfg(test)]
mod tests {
    use super::HistoryService;
    use crate::tests::utils::setup_test_db::setup_test_db_with_comic;

    async fn setup() -> (sqlx::SqlitePool, HistoryService) {
        let pool = setup_test_db_with_comic().await;
        let service = HistoryService::new(pool.clone());
        (pool, service)
    }

    #[tokio::test]
    async fn teste_atualiza_progresso_de_leitura() {
        let (pool, service) = setup().await;

        sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)")
            .execute(&pool)
            .await
            .unwrap();

        let result = service.update_progress(1, 1, 10, false).await.unwrap();

        assert_eq!(result.comic_directory_id, 1);
        assert_eq!(result.chapter_archive_id, 1);
        assert_eq!(result.last_page, 10);
        assert!(!result.is_completed);
    }

    #[tokio::test]
    async fn teste_limpa_historico() {
        let (pool, service) = setup().await;

        sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)")
            .execute(&pool)
            .await
            .unwrap();

        service.update_progress(1, 1, 10, false).await.unwrap();

        let before = service.reading_repo.find_all_ordered().await.unwrap();
        assert_eq!(before.len(), 1);

        service.clear().await.unwrap();

        let after = service.reading_repo.find_all_ordered().await.unwrap();
        assert_eq!(after.len(), 0);
    }
}
