use sqlx::SqlitePool;

use crate::{
    data::{
        models::history::chapter_read::ChapterRead,
        repositories::{Entity, Repository},
    },
    infra::error::DbError,
};

#[derive(Clone)]
pub struct ChapterReadRepository {
    pub base: Repository<ChapterRead>,
    pool: SqlitePool,
}

impl ChapterReadRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool.clone()), pool }
    }

    /// Registra um capítulo como lido. Ignora silenciosamente se já existir
    /// (chave composta `comic_directory_id` + `chapter_archive_id`).
    pub async fn insert_or_ignore(&self, chapter_read: &ChapterRead) -> Result<(), DbError> {
        let cols = ChapterRead::columns().join(", ");
        let table = ChapterRead::table_name();

        sqlx::query(&format!(
            "INSERT INTO {table} ({cols})
             VALUES (?, ?, ?)
             ON CONFLICT(comic_directory_id, chapter_archive_id) DO NOTHING"
        ))
        .bind(chapter_read.comic_directory_id)
        .bind(chapter_read.chapter_archive_id)
        .bind(chapter_read.created_at)
        .execute(&self.pool)
        .await?;

        Ok(())
    }

    /// Retorna os IDs dos capítulos lidos de um quadrinho.
    pub async fn find_ids_by_comic(
        &self, comic_directory_id: i64,
    ) -> Result<Vec<i64>, DbError> {
        let table = ChapterRead::table_name();

        let rows = sqlx::query_as::<_, (i64,)>(&format!(
            "SELECT chapter_archive_id FROM {} WHERE comic_directory_id = ?",
            table
        ))
        .bind(comic_directory_id)
        .fetch_all(&self.pool)
        .await?;

        Ok(rows.into_iter().map(|(id,)| id).collect())
    }
}

#[cfg(test)]
mod tests {
    use super::ChapterReadRepository;
    use crate::{
        data::models::history::chapter_read::ChapterRead,
        tests::utils::setup_test_db::setup_test_db_with_comic,
    };

    fn capitulo_lido(comic_directory_id: i64, chapter_archive_id: i64) -> ChapterRead {
        ChapterRead { comic_directory_id, chapter_archive_id, created_at: 1000 }
    }

    async fn setup() -> (sqlx::SqlitePool, ChapterReadRepository) {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterReadRepository::new(pool.clone());
        (pool, repo)
    }

    #[tokio::test]
    async fn teste_insert_or_ignore_registra_leitura() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)")
            .execute(&pool)
            .await
            .unwrap();

        repo.insert_or_ignore(&capitulo_lido(1, 1)).await.unwrap();

        let ids = repo.find_ids_by_comic(1).await.unwrap();
        assert_eq!(ids.len(), 1);
        assert_eq!(ids[0], 1);
    }

    #[tokio::test]
    async fn teste_insert_or_ignore_nao_duplica() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)")
            .execute(&pool)
            .await
            .unwrap();

        repo.insert_or_ignore(&capitulo_lido(1, 1)).await.unwrap();
        repo.insert_or_ignore(&capitulo_lido(1, 1)).await.unwrap();

        let ids = repo.find_ids_by_comic(1).await.unwrap();
        assert_eq!(ids.len(), 1);
    }

    #[tokio::test]
    async fn teste_find_ids_by_comic_vazio() {
        let (_, repo) = setup().await;
        let ids = repo.find_ids_by_comic(1).await.unwrap();
        assert!(ids.is_empty());
    }
}
