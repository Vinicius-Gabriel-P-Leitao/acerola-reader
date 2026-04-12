use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::repositories::base::{Entity, Repository};
use crate::infra::error::translations::db_error::DbError;
use sqlx::SqlitePool;

pub struct ComicRepository {
    pub base: Repository<ComicDirectory>,
    pool: SqlitePool,
}

impl ComicRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            base: Repository::new(pool.clone()),
            pool,
        }
    }

    pub async fn find_by_name(&self, name: &str) -> Result<Option<ComicDirectory>, DbError> {
        let table = ComicDirectory::table_name();
        let cols = ComicDirectory::columns().join(", ");

        
        let result = sqlx::query_as::<_, ComicDirectory>(&format!(
            "SELECT {} FROM {} WHERE name = ?",
            cols, table
        ))
        .bind(name)
        .fetch_optional(&self.pool)
        .await?;

        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::ComicRepository;
    use crate::data::models::archive::comic_directory::ComicDirectory;
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

    async fn setup() -> ComicRepository {
        ComicRepository::new(setup_test_db().await)
    }

    #[tokio::test]
    async fn teste_inserir_e_buscar_todos() {
        let repo = setup().await;

        let inserted = repo.base.insert(&berserk()).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.name, "Berserk");

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn teste_buscar_por_nome() {
        let repo = setup().await;

        repo.base.insert(&berserk()).await.unwrap();

        let result = repo.find_by_name("Berserk").await.unwrap();
        assert!(result.is_some());
        assert_eq!(result.unwrap().name, "Berserk");
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let repo = setup().await;

        repo.base.insert(&berserk()).await.unwrap();

        let updated = ComicDirectory {
            name: "Berserk Deluxe".to_string(),
            ..berserk()
        };
        let result = repo.base.update(&updated).await.unwrap();

        assert_eq!(result.name, "Berserk Deluxe");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let repo = setup().await;

        repo.base.insert(&berserk()).await.unwrap();
        repo.base.delete(1).await.unwrap();

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }

    #[tokio::test]
    async fn teste_buscar_por_nome_inexistente() {
        let repo = setup().await;

        let result = repo.find_by_name("Inexistente").await.unwrap();

        assert!(result.is_none());
    }

    #[tokio::test]
    async fn teste_erro_ao_inserir_duplicado() {
        let repo = setup().await;

        repo.base.insert(&berserk()).await.unwrap();
        let result = repo.base.insert(&berserk()).await;

        assert!(
            matches!(result, Err(DbError::UniqueViolation)),
            "Deveria ter retornado UniqueViolation, mas veio: {:?}",
            result
        );
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let repo = setup().await;

        let result = repo.base.update(&berserk()).await;

        assert!(
            matches!(result, Err(DbError::NotFound)),
            "Deveria ter retornado NotFound, mas veio: {:?}",
            result
        );
    }
}
