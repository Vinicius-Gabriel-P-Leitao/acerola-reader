use crate::data::models::archive::chapter_archive::ChapterArchive;
use crate::data::repositories::base::{ Entity, Repository };
use crate::infra::error::translations::db_error::DbError;
use sqlx::SqlitePool;

pub struct ChapterRepository {
    pub base: Repository<ChapterArchive>,
    pool: SqlitePool,
}

impl ChapterRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            base: Repository::new(pool.clone()),
            pool,
        }
    }

    /// Retorna capítulos de um diretório paginados, ordenados por `chapter_sort`.
    ///
    /// A ordenação separa a parte inteira e decimal do campo para garantir que
    /// `0.9` venha antes de `0.10` — ordenação numérica, não lexicográfica.
    // prettier-ignore
    pub async fn get_chapters_paged(
        &self,
        folder_id: i64,
        page_size: i64,
        offset: i64,
    ) -> Result<Vec<ChapterArchive>, DbError> {
        let cols = ChapterArchive::columns().iter().map(|col| format!("ca.{}", col)).collect::<Vec<_>>().join(", ");

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
        )).bind(folder_id).bind(page_size).bind(offset).fetch_all(&self.pool).await?;

        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::{ ChapterArchive, ChapterRepository };
    use crate::data::models::archive::comic_directory::ComicDirectory;
    use crate::data::repositories::base::Repository;
    use crate::infra::error::translations::db_error::DbError;
    use crate::tests::utils::setup_test_db::setup_test_db;

    fn berserk() -> ComicDirectory {
        ComicDirectory {
            id: 1,
            name: "Berserk".to_string(),
            path: "/quadrinhos/berserk".to_string(),
            cover: None,
            banner: None,
            last_modified: 1700000000,
            chapter_template_fk: None,
            external_sync_enabled: true,
            hidden: false,
        }
    }

    fn chapter(id: i64, chapter_sort: &str) -> ChapterArchive {
        ChapterArchive {
            id,
            chapter: format!("Capítulo {}", id),
            path: format!("/quadrinhos/berserk/cap{}", id),
            chapter_sort: chapter_sort.to_string(),
            fast_hash: None,
            comic_directory_fk: 1,
            last_modified: 123456789,
        }
    }

    async fn setup() -> ChapterRepository {
        let pool = setup_test_db().await;
        Repository::<ComicDirectory>::new(pool.clone()).insert(&berserk()).await.unwrap();
        ChapterRepository::new(pool)
    }

    #[tokio::test]
    async fn teste_inserir_e_buscar_todos() {
        let repo = setup().await;

        let inserted = repo.base.insert(&chapter(1, "001")).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.chapter, "Capítulo 1");

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let repo = setup().await;

        repo.base.insert(&chapter(1, "001")).await.unwrap();

        let updated = ChapterArchive {
            chapter: "Capítulo Especial".to_string(),
            ..chapter(1, "001")
        };
        let result = repo.base.update(&updated).await.unwrap();

        assert_eq!(result.chapter, "Capítulo Especial");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let repo = setup().await;

        repo.base.insert(&chapter(1, "001")).await.unwrap();
        repo.base.delete(1).await.unwrap();

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }

    #[tokio::test]
    async fn teste_buscar_capitulos_paginados_ordenacao() {
        let repo = setup().await;

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
        let repo = setup().await;

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
    async fn teste_buscar_capitulos_pasta_sem_registros() {
        let repo = setup().await;

        let result = repo.get_chapters_paged(1, 10, 0).await.unwrap();

        assert!(result.is_empty());
    }

    #[tokio::test]
    async fn teste_erro_ao_inserir_duplicado() {
        let repo = setup().await;

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
        let repo = setup().await;

        let result = repo.base.update(&chapter(999, "001")).await;

        assert!(
            matches!(result, Err(DbError::NotFound)),
            "Deveria ter retornado NotFound, mas veio: {:?}",
            result
        );
    }

    #[tokio::test]
    async fn teste_erro_fk_invalida_ao_inserir() {
        let pool = setup_test_db().await;
        sqlx::query("PRAGMA foreign_keys = ON").execute(&pool).await.unwrap();
        let repo = ChapterRepository::new(pool);

        let invalid = ChapterArchive {
            comic_directory_fk: 999,
            ..chapter(1, "001")
        };
        let result = repo.base.insert(&invalid).await;

        assert!(
            matches!(result, Err(DbError::ForeignKeyViolation)),
            "Deveria ter retornado ForeignKeyViolation, mas veio: {:?}",
            result
        );
    }
}
