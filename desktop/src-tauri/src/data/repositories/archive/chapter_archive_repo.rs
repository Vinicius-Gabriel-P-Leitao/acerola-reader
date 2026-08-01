use std::collections::HashMap;

use sqlx::{Row, SqlitePool};

use crate::{
    data::{
        models::{
            archive::chapter_archive::ChapterArchive,
            relations::chapter_with_volume::ChapterArchiveWithVolume,
        },
        repositories::Repository,
    },
    infra::error::DbError,
};

#[derive(Debug, Clone, Copy, Default)]
pub enum ChapterSortCriteria {
    #[default]
    NumberAsc,
    NumberDesc,
    ModifiedAsc,
    ModifiedDesc,
}

pub struct ChapterRepository {
    pub base: Repository<ChapterArchive>,
    pool: SqlitePool,
}

impl ChapterRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool.clone()), pool }
    }

    pub async fn get_all_counts(&self) -> Result<HashMap<i64, i64>, DbError> {
        let counts = sqlx::query("SELECT comic_directory_fk, COUNT(*) as count FROM chapter_archive GROUP BY comic_directory_fk")
            .fetch_all(&self.pool)
            .await?
            .into_iter()
            .map(|row| (row.get::<i64, _>("comic_directory_fk"), row.get::<i64, _>("count")))
            .collect();

        Ok(counts)
    }

    pub async fn count_by_directory_id(&self, comic_directory_fk: i64) -> Result<i64, DbError> {
        let result =
            sqlx::query("SELECT COUNT(*) FROM chapter_archive WHERE comic_directory_fk = ?")
                .bind(comic_directory_fk)
                .fetch_one(&self.pool)
                .await?;
        Ok(result.get(0))
    }

    pub async fn count_by_directory_id_with_search(
        &self, comic_directory_fk: i64, search_query: &str,
    ) -> Result<i64, DbError> {
        let search_pattern = format!("%{}%", search_query);
        let result = sqlx::query(
            "SELECT COUNT(*) FROM chapter_archive WHERE comic_directory_fk = ? AND chapter LIKE ?",
        )
        .bind(comic_directory_fk)
        .bind(&search_pattern)
        .fetch_one(&self.pool)
        .await?;
        Ok(result.get(0))
    }

    pub async fn count_root_chapters(&self, comic_directory_fk: i64) -> Result<i64, DbError> {
        let result = sqlx::query("SELECT COUNT(*) FROM chapter_archive WHERE comic_directory_fk = ? AND volume_id_fk IS NULL")
            .bind(comic_directory_fk)
            .fetch_one(&self.pool)
            .await?;
        Ok(result.get(0))
    }

    pub async fn get_total_count_by_volume(&self, volume_id_fk: i64) -> Result<i64, DbError> {
        let result = sqlx::query("SELECT COUNT(*) FROM chapter_archive WHERE volume_id_fk = ?")
            .bind(volume_id_fk)
            .fetch_one(&self.pool)
            .await?;
        Ok(result.get(0))
    }

    fn order_clause(criteria: ChapterSortCriteria) -> String {
        match criteria {
            ChapterSortCriteria::NumberAsc => "ORDER BY CAST(ca.chapter_sort AS INTEGER) ASC, CAST(CASE WHEN ca.chapter_sort LIKE '%.%' THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1) ELSE 0 END AS INTEGER) ASC, (ca.is_special OR COALESCE(va.is_special, 0)) ASC".to_string(),
            ChapterSortCriteria::NumberDesc => "ORDER BY CAST(ca.chapter_sort AS INTEGER) DESC, CAST(CASE WHEN ca.chapter_sort LIKE '%.%' THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1) ELSE 0 END AS INTEGER) DESC, (ca.is_special OR COALESCE(va.is_special, 0)) ASC".to_string(),
            ChapterSortCriteria::ModifiedAsc => "ORDER BY ca.last_modified ASC".to_string(),
            ChapterSortCriteria::ModifiedDesc => "ORDER BY ca.last_modified DESC".to_string(),
        }
    }

    pub async fn get_chapters_by_directory(
        &self, comic_directory_fk: i64, page_size: i64, offset: i64, criteria: ChapterSortCriteria,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let order = Self::order_clause(criteria);
        let sql = format!(
            "SELECT ca.*, va.name AS volume_name, va.volume_sort, va.is_special AS volume_is_special FROM chapter_archive ca LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id WHERE ca.comic_directory_fk = ? {} LIMIT ? OFFSET ?",
            order
        );

        sqlx::query_as::<_, ChapterArchiveWithVolume>(&sql)
            .bind(comic_directory_fk)
            .bind(page_size)
            .bind(offset)
            .fetch_all(&self.pool)
            .await
            .map_err(Into::into)
    }

    pub async fn get_chapters_by_directory_with_search(
        &self, comic_directory_fk: i64, page_size: i64, offset: i64, search_query: &str,
        criteria: ChapterSortCriteria,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let search_pattern = format!("%{}%", search_query);
        let order = Self::order_clause(criteria);
        let sql = format!(
            "SELECT ca.*, va.name AS volume_name, va.volume_sort, va.is_special AS volume_is_special FROM chapter_archive ca LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id WHERE ca.comic_directory_fk = ? AND ca.chapter LIKE ? {} LIMIT ? OFFSET ?",
            order
        );

        sqlx::query_as::<_, ChapterArchiveWithVolume>(&sql)
            .bind(comic_directory_fk)
            .bind(&search_pattern)
            .bind(page_size)
            .bind(offset)
            .fetch_all(&self.pool)
            .await
            .map_err(Into::into)
    }

    pub async fn get_chapters_by_volume(
        &self, comic_directory_fk: i64, volume_id_fk: i64, page_size: i64, offset: i64,
        criteria: ChapterSortCriteria,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let order = Self::order_clause(criteria);
        let sql = format!(
            "SELECT ca.*, va.name AS volume_name, va.volume_sort, va.is_special AS volume_is_special FROM chapter_archive ca LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id WHERE ca.comic_directory_fk = ? AND ca.volume_id_fk = ? {} LIMIT ? OFFSET ?",
            order
        );

        sqlx::query_as::<_, ChapterArchiveWithVolume>(&sql)
            .bind(comic_directory_fk)
            .bind(volume_id_fk)
            .bind(page_size)
            .bind(offset)
            .fetch_all(&self.pool)
            .await
            .map_err(Into::into)
    }
}

#[cfg(test)]
mod tests {
    use super::{ChapterArchive, ChapterRepository, ChapterSortCriteria};
    use crate::{
        infra::error::DbError,
        tests::utils::setup_test_db::{setup_test_db_with_comic, setup_test_db_with_volumes},
    };

    fn chapter(id: i64, chapter_sort: &str) -> ChapterArchive {
        ChapterArchive {
            id,
            chapter: format!("Capítulo {}", id),
            path: format!("/quadrinhos/berserk/cap{}", id),
            chapter_sort: chapter_sort.to_string(),
            is_special: false,
            checksum: None,
            comic_directory_fk: 1,
            volume_id_fk: None,
            last_modified: 123456789,
        }
    }

    #[tokio::test]
    async fn teste_inserir_e_buscar_todos() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        let inserted = repo.base.insert(&chapter(1, "001")).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.chapter, "Capítulo 1");

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn teste_contagens() {
        let pool = setup_test_db_with_volumes().await;
        let repo = ChapterRepository::new(pool);
        repo.base.insert(&chapter(1, "1")).await.unwrap();
        repo.base.insert(&chapter(2, "2")).await.unwrap();
        repo.base
            .insert(&ChapterArchive { id: 3, volume_id_fk: Some(1), ..chapter(3, "3") })
            .await
            .unwrap();

        assert_eq!(repo.count_by_directory_id(1).await.unwrap(), 3);
        assert_eq!(repo.count_root_chapters(1).await.unwrap(), 2);
        assert_eq!(repo.get_total_count_by_volume(1).await.unwrap(), 1);
    }

    #[tokio::test]
    async fn teste_ordenacao_por_numero_asc() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        repo.base.insert(&chapter(1, "0.9")).await.unwrap();
        repo.base.insert(&chapter(2, "0.10")).await.unwrap();
        repo.base.insert(&chapter(3, "1.0")).await.unwrap();
        repo.base.insert(&chapter(4, "0.1")).await.unwrap();

        let result =
            repo.get_chapters_by_directory(1, 10, 0, ChapterSortCriteria::NumberAsc).await.unwrap();

        assert_eq!(result.len(), 4);
        assert_eq!(result[0].chapter_sort, "0.1");
        assert_eq!(result[1].chapter_sort, "0.9");
        assert_eq!(result[2].chapter_sort, "0.10");
        assert_eq!(result[3].chapter_sort, "1.0");
    }

    #[tokio::test]
    async fn teste_ordenacao_por_numero_desc() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        repo.base.insert(&chapter(1, "0.9")).await.unwrap();
        repo.base.insert(&chapter(2, "0.10")).await.unwrap();
        repo.base.insert(&chapter(3, "1.0")).await.unwrap();
        repo.base.insert(&chapter(4, "0.1")).await.unwrap();

        let result = repo
            .get_chapters_by_directory(1, 10, 0, ChapterSortCriteria::NumberDesc)
            .await
            .unwrap();

        assert_eq!(result.len(), 4);
        assert_eq!(result[0].chapter_sort, "1.0");
        assert_eq!(result[1].chapter_sort, "0.10");
        assert_eq!(result[2].chapter_sort, "0.9");
        assert_eq!(result[3].chapter_sort, "0.1");
    }

    #[tokio::test]
    async fn teste_ordenacao_por_modificado_asc() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        let mut c1 = chapter(1, "1");
        c1.last_modified = 300;
        let mut c2 = chapter(2, "2");
        c2.last_modified = 100;
        let mut c3 = chapter(3, "3");
        c3.last_modified = 200;

        repo.base.insert(&c1).await.unwrap();
        repo.base.insert(&c2).await.unwrap();
        repo.base.insert(&c3).await.unwrap();

        let result = repo
            .get_chapters_by_directory(1, 10, 0, ChapterSortCriteria::ModifiedAsc)
            .await
            .unwrap();

        assert_eq!(result.len(), 3);
        assert_eq!(result[0].last_modified, 100);
        assert_eq!(result[1].last_modified, 200);
        assert_eq!(result[2].last_modified, 300);
    }

    #[tokio::test]
    async fn teste_ordenacao_por_modificado_desc() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        let mut c1 = chapter(1, "1");
        c1.last_modified = 300;
        let mut c2 = chapter(2, "2");
        c2.last_modified = 100;
        let mut c3 = chapter(3, "3");
        c3.last_modified = 200;

        repo.base.insert(&c1).await.unwrap();
        repo.base.insert(&c2).await.unwrap();
        repo.base.insert(&c3).await.unwrap();

        let result = repo
            .get_chapters_by_directory(1, 10, 0, ChapterSortCriteria::ModifiedDesc)
            .await
            .unwrap();

        assert_eq!(result.len(), 3);
        assert_eq!(result[0].last_modified, 300);
        assert_eq!(result[1].last_modified, 200);
        assert_eq!(result[2].last_modified, 100);
    }

    #[tokio::test]
    async fn teste_erro_ao_inserir_duplicado() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        repo.base.insert(&chapter(1, "001")).await.unwrap();
        let result = repo.base.insert(&chapter(1, "001")).await;

        assert!(
            matches!(result, Err(DbError::UniqueViolation)),
            "Deveria ter retornado UniqueViolation, mas veio: {:?}",
            result
        );
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        let result = repo.base.update(&chapter(999, "001")).await;

        assert!(
            matches!(result, Err(DbError::NotFound)),
            "Deveria ter retornado NotFound, mas veio: {:?}",
            result
        );
    }

    #[tokio::test]
    async fn teste_erro_fk_invalida_ao_inserir() {
        let pool = setup_test_db_with_comic().await;
        sqlx::query("PRAGMA foreign_keys = ON").execute(&pool).await.unwrap();
        let repo = ChapterRepository::new(pool);

        let invalid = ChapterArchive { comic_directory_fk: 999, ..chapter(1, "001") };
        let result = repo.base.insert(&invalid).await;

        assert!(
            matches!(result, Err(DbError::ForeignKeyViolation)),
            "Deveria ter retornado ForeignKeyViolation, mas veio: {:?}",
            result
        );
    }
}
