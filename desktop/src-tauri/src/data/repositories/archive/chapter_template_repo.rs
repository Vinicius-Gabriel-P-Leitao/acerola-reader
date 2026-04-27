use crate::data::models::archive::chapter_template::ChapterTemplate;
use crate::data::repositories::base::Repository;
use sqlx::SqlitePool;

pub struct ChapterTemplateRepository {
    pub base: Repository<ChapterTemplate>,
}

impl ChapterTemplateRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool) }
    }
}

#[cfg(test)]
mod tests {
    use super::ChapterTemplateRepository;
    use crate::data::models::archive::chapter_template::ChapterTemplate;
    use crate::infra::error::DbError;
    use crate::tests::utils::setup_test_db::setup_test_db;

    fn template() -> ChapterTemplate {
        ChapterTemplate {
            id: 1,
            label: "Default Preset".to_string(),
            pattern: "Chapter %d".to_string(),
            is_default: true,
            priority: 10,
        }
    }

    async fn setup() -> ChapterTemplateRepository {
        ChapterTemplateRepository::new(setup_test_db().await)
    }

    #[tokio::test]
    async fn teste_inserir_e_buscar_todos() {
        let repo = setup().await;

        let inserted = repo.base.insert(&template()).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.label, "Default Preset");

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let repo = setup().await;

        repo.base.insert(&template()).await.unwrap();

        let updated = ChapterTemplate { label: "Preset Deluxe".to_string(), ..template() };
        let result = repo.base.update(&updated).await.unwrap();

        assert_eq!(result.label, "Preset Deluxe");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let repo = setup().await;

        repo.base.insert(&template()).await.unwrap();
        repo.base.delete(1).await.unwrap();

        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }

    #[tokio::test]
    async fn teste_erro_ao_inserir_duplicado() {
        let repo = setup().await;

        repo.base.insert(&template()).await.unwrap();
        let result = repo.base.insert(&template()).await;

        assert!(
            matches!(result, Err(DbError::UniqueViolation)),
            "Deveria ter retornado UniqueViolation, mas veio: {:?}",
            result
        );
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let repo = setup().await;

        let result = repo.base.update(&template()).await;

        assert!(
            matches!(result, Err(DbError::NotFound)),
            "Deveria ter retornado NotFound, mas veio: {:?}",
            result
        );
    }
}
