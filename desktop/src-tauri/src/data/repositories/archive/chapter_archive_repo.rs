use crate::data::models::archive::chapter_archive::ChapterArchive;
use crate::data::repositories::{Entity, Repository};
use crate::infra::error::DbError;
use sqlx::{Row, SqlitePool};

use std::collections::HashMap;

#[derive(Debug, sqlx::FromRow, Clone)]
pub struct ChapterArchiveWithVolume {
    pub id: i64,
    pub chapter: String,
    pub path: String,
    pub chapter_sort: String,
    pub is_special: bool,
    pub checksum: Option<String>,
    pub fast_hash: Option<String>,
    pub comic_directory_fk: i64,
    pub volume_id_fk: Option<i64>,
    pub last_modified: i64,
    // Volume fields
    pub volume_name: Option<String>,
    pub volume_sort: Option<String>,
    pub volume_is_special: Option<bool>,
}

pub struct ChapterRepository {
    pub base: Repository<ChapterArchive>,
    pool: SqlitePool,
}

impl ChapterRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool.clone()), pool }
    }

    /// Retorna um mapeamento de `directory_id` para o número total de capítulos.
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

    pub async fn get_chapters_by_directory_paged(
        &self, comic_directory_fk: i64, page_size: i64, offset: i64,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let result = sqlx::query_as::<_, ChapterArchiveWithVolume>(
            "SELECT 
                ca.*,
                va.name AS volume_name,
                va.volume_sort,
                va.is_special AS volume_is_special
             FROM chapter_archive ca
             LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id
             WHERE ca.comic_directory_fk = ?
             ORDER BY 
                CAST(COALESCE(va.volume_sort, '0') AS INTEGER) ASC,
                CAST(
                    CASE 
                        WHEN va.volume_sort LIKE '%.%' 
                        THEN SUBSTR(va.volume_sort, INSTR(va.volume_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) ASC,
                CAST(ca.chapter_sort AS INTEGER) ASC, 
                CAST(
                    CASE 
                        WHEN ca.chapter_sort LIKE '%.%' 
                        THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) ASC,
                (ca.is_special OR COALESCE(va.is_special, 0)) ASC
             LIMIT ? OFFSET ?",
        )
        .bind(comic_directory_fk)
        .bind(page_size)
        .bind(offset)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }

    pub async fn get_chapters_by_directory_paged_desc(
        &self, comic_directory_fk: i64, page_size: i64, offset: i64,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let result = sqlx::query_as::<_, ChapterArchiveWithVolume>(
            "SELECT 
                ca.*,
                va.name AS volume_name,
                va.volume_sort,
                va.is_special AS volume_is_special
             FROM chapter_archive ca
             LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id
             WHERE ca.comic_directory_fk = ?
             ORDER BY 
                CAST(COALESCE(va.volume_sort, '0') AS INTEGER) DESC,
                CAST(
                    CASE 
                        WHEN va.volume_sort LIKE '%.%' 
                        THEN SUBSTR(va.volume_sort, INSTR(va.volume_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) DESC,
                CAST(ca.chapter_sort AS INTEGER) DESC, 
                CAST(
                    CASE 
                        WHEN ca.chapter_sort LIKE '%.%' 
                        THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) DESC,
                (ca.is_special OR COALESCE(va.is_special, 0)) ASC
             LIMIT ? OFFSET ?",
        )
        .bind(comic_directory_fk)
        .bind(page_size)
        .bind(offset)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }

    pub async fn get_chapters_by_volume_paged(
        &self, comic_directory_fk: i64, volume_id_fk: i64, page_size: i64, offset: i64,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let result = sqlx::query_as::<_, ChapterArchiveWithVolume>(
            "SELECT 
                ca.*,
                va.name AS volume_name,
                va.volume_sort,
                va.is_special AS volume_is_special
             FROM chapter_archive ca
             LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id
             WHERE ca.comic_directory_fk = ? AND ca.volume_id_fk = ?
             ORDER BY 
                CAST(ca.chapter_sort AS INTEGER) ASC, 
                CAST(
                    CASE 
                        WHEN ca.chapter_sort LIKE '%.%' 
                        THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) ASC,
                (ca.is_special OR COALESCE(va.is_special, 0)) ASC
             LIMIT ? OFFSET ?",
        )
        .bind(comic_directory_fk)
        .bind(volume_id_fk)
        .bind(page_size)
        .bind(offset)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }

    pub async fn get_chapters_by_volume_paged_desc(
        &self, comic_directory_fk: i64, volume_id_fk: i64, page_size: i64, offset: i64,
    ) -> Result<Vec<ChapterArchiveWithVolume>, DbError> {
        let result = sqlx::query_as::<_, ChapterArchiveWithVolume>(
            "SELECT 
                ca.*,
                va.name AS volume_name,
                va.volume_sort,
                va.is_special AS volume_is_special
             FROM chapter_archive ca
             LEFT JOIN volume_archive va ON ca.volume_id_fk = va.id
             WHERE ca.comic_directory_fk = ? AND ca.volume_id_fk = ?
             ORDER BY 
                CAST(ca.chapter_sort AS INTEGER) DESC, 
                CAST(
                    CASE 
                        WHEN ca.chapter_sort LIKE '%.%' 
                        THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) DESC,
                (ca.is_special OR COALESCE(va.is_special, 0)) ASC
             LIMIT ? OFFSET ?",
        )
        .bind(comic_directory_fk)
        .bind(volume_id_fk)
        .bind(page_size)
        .bind(offset)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }

    /// Retorna capítulos de um diretório paginados, ordenados por `chapter_sort`.
    pub async fn get_chapters_paged(
        &self, comic_directory_fk: i64, page_size: i64, offset: i64,
    ) -> Result<Vec<ChapterArchive>, DbError> {
        let cols = ChapterArchive::columns()
            .iter()
            .map(|col| format!("ca.{}", col))
            .collect::<Vec<_>>()
            .join(", ");

        let result = sqlx::query_as::<_, ChapterArchive>(&format!(
            "SELECT {cols}
             FROM chapter_archive ca
             WHERE ca.comic_directory_fk = ?
             ORDER BY
                 CAST(ca.chapter_sort AS INTEGER) ASC,
                 CAST(
                     CASE
                         WHEN ca.chapter_sort LIKE '%.%'
                         THEN SUBSTR(ca.chapter_sort, INSTR(ca.chapter_sort, '.') + 1)
                         ELSE 0
                     END AS INTEGER
                 ) ASC
             LIMIT ? OFFSET ?"
        ))
        .bind(comic_directory_fk)
        .bind(page_size)
        .bind(offset)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::{ChapterArchive, ChapterRepository};
    use crate::infra::error::DbError;
    use crate::tests::utils::setup_test_db::{
        setup_test_db_with_comic, setup_test_db_with_volumes,
    };

    fn chapter(id: i64, chapter_sort: &str) -> ChapterArchive {
        ChapterArchive {
            id,
            chapter: format!("Capítulo {}", id),
            path: format!("/quadrinhos/berserk/cap{}", id),
            chapter_sort: chapter_sort.to_string(),
            is_special: false,
            checksum: None,
            fast_hash: None,
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
    async fn teste_atualizar() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        repo.base.insert(&chapter(1, "001")).await.unwrap();

        let updated =
            ChapterArchive { chapter: "Capítulo Especial".to_string(), ..chapter(1, "001") };
        let result = repo.base.update(&updated).await.unwrap();

        assert_eq!(result.chapter, "Capítulo Especial");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        repo.base.insert(&chapter(1, "001")).await.unwrap();
        repo.base.delete(1).await.unwrap();

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }

    #[tokio::test]
    async fn teste_buscar_capitulos_paginados_ordenacao() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        // Insere fora de ordem para validar que a ordenação funciona
        repo.base.insert(&chapter(1, "0.9")).await.unwrap();
        repo.base.insert(&chapter(2, "0.10")).await.unwrap();
        repo.base.insert(&chapter(3, "1.0")).await.unwrap();
        repo.base.insert(&chapter(4, "0.1")).await.unwrap();

        let result = repo.get_chapters_paged(1, 10, 0).await.unwrap();

        assert_eq!(result.len(), 4);
        assert_eq!(result[0].chapter_sort, "0.1"); // 0 inteiro, 1 decimal
        assert_eq!(result[1].chapter_sort, "0.9"); // 0 inteiro, 9 decimal
        assert_eq!(result[2].chapter_sort, "0.10"); // 0 inteiro, 10 decimal
        assert_eq!(result[3].chapter_sort, "1.0"); // 1 inteiro
    }

    #[tokio::test]
    async fn teste_buscar_capitulos_paginados_paginacao() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        repo.base.insert(&chapter(1, "0.1")).await.unwrap();
        repo.base.insert(&chapter(2, "0.2")).await.unwrap();
        repo.base.insert(&chapter(3, "0.3")).await.unwrap();

        let page1 = repo.get_chapters_paged(1, 2, 0).await.unwrap();
        let page2 = repo.get_chapters_paged(1, 2, 2).await.unwrap();

        assert_eq!(page1.len(), 2);
        assert_eq!(page2.len(), 1);
        assert_eq!(page2[0].chapter_sort, "0.3");
    }

    #[tokio::test]
    async fn teste_counts() {
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
    async fn teste_get_chapters_by_directory_paged_com_volumes() {
        let pool = setup_test_db_with_volumes().await;
        let repo = ChapterRepository::new(pool);

        repo.base
            .insert(&ChapterArchive { id: 1, volume_id_fk: Some(2), ..chapter(1, "1") })
            .await
            .unwrap();
        repo.base
            .insert(&ChapterArchive { id: 2, volume_id_fk: Some(1), ..chapter(2, "2") })
            .await
            .unwrap();
        repo.base
            .insert(&ChapterArchive { id: 3, volume_id_fk: None, ..chapter(3, "0.5") })
            .await
            .unwrap();

        // Ordem ASC: Volume NULL (sort '0') -> Volume 1 -> Volume 2
        let result = repo.get_chapters_by_directory_paged(1, 10, 0).await.unwrap();
        assert_eq!(result.len(), 3);
        assert_eq!(result[0].id, 3); // Root chapter (Volume sort '0')
        assert_eq!(result[1].id, 2); // Vol 1
        assert_eq!(result[2].id, 1); // Vol 2

        // Ordem DESC
        let result_desc = repo.get_chapters_by_directory_paged_desc(1, 10, 0).await.unwrap();
        assert_eq!(result_desc[0].id, 1); // Vol 2
        assert_eq!(result_desc[1].id, 2); // Vol 1
        assert_eq!(result_desc[2].id, 3); // Root
    }

    #[tokio::test]
    async fn teste_get_chapters_by_volume_paged() {
        let pool = setup_test_db_with_volumes().await;
        let repo = ChapterRepository::new(pool);

        repo.base
            .insert(&ChapterArchive { id: 1, volume_id_fk: Some(1), ..chapter(1, "1") })
            .await
            .unwrap();
        repo.base
            .insert(&ChapterArchive { id: 2, volume_id_fk: Some(1), ..chapter(2, "2") })
            .await
            .unwrap();
        repo.base
            .insert(&ChapterArchive { id: 3, volume_id_fk: Some(2), ..chapter(3, "3") })
            .await
            .unwrap();

        let result = repo.get_chapters_by_volume_paged(1, 1, 10, 0).await.unwrap();
        assert_eq!(result.len(), 2);
        assert_eq!(result[0].id, 1);
        assert_eq!(result[1].id, 2);
    }

    #[tokio::test]
    async fn teste_buscar_capitulos_pasta_sem_registros() {
        let pool = setup_test_db_with_comic().await;
        let repo = ChapterRepository::new(pool);

        let result = repo.get_chapters_paged(1, 10, 0).await.unwrap();

        assert!(result.is_empty());
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
        // Ativa FKs explicitamente se necessário, mas setup_test_db já deve lidar com isso se for o caso
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
