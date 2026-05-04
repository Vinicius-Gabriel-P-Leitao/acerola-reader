use crate::data::models::archive::archive_template::ArchiveTemplate;
use crate::data::repositories::Repository;
use sqlx::SqlitePool;

pub struct ArchiveTemplateRepository {
    pub base: Repository<ArchiveTemplate>,
}

impl ArchiveTemplateRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool) }
    }
}

#[cfg(test)]
mod tests {
    use super::ArchiveTemplateRepository;
    use crate::data::models::archive::archive_template::{ArchiveTemplate, SortType};
    use crate::infra::error::DbError;
    use crate::tests::utils::setup_test_db::setup_test_db;

    fn chapter_template() -> ArchiveTemplate {
        ArchiveTemplate {
            id: 1,
            label: "Ch. 01.*.".to_string(),
            pattern: "Ch. {chapter}{decimal}.*.{extension}".to_string(),
            sort_type: SortType::Chapter,
            is_default: true,
            priority: 0,
        }
    }

    fn volume_template() -> ArchiveTemplate {
        ArchiveTemplate {
            id: 2,
            label: "Vol. 01".to_string(),
            pattern: "Vol. {volume}{decimal}".to_string(),
            sort_type: SortType::Volume,
            is_default: true,
            priority: 0,
        }
    }

    async fn setup() -> ArchiveTemplateRepository {
        ArchiveTemplateRepository::new(setup_test_db().await)
    }

    #[tokio::test]
    async fn teste_inserir_chapter_template() {
        let repo = setup().await;
        let inserted = repo.base.insert(&chapter_template()).await.unwrap();
        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.sort_type, SortType::Chapter);
    }

    #[tokio::test]
    async fn teste_inserir_volume_template() {
        let repo = setup().await;
        let inserted = repo.base.insert(&volume_template()).await.unwrap();
        assert_eq!(inserted.sort_type, SortType::Volume);
    }

    #[tokio::test]
    async fn teste_buscar_todos() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        repo.base.insert(&volume_template()).await.unwrap();
        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 2);
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        let updated = ArchiveTemplate { label: "Updated".to_string(), ..chapter_template() };
        let result = repo.base.update(&updated).await.unwrap();
        assert_eq!(result.label, "Updated");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        repo.base.delete(1).await.unwrap();
        assert_eq!(repo.base.find_all().await.unwrap().len(), 0);
    }

    #[tokio::test]
    async fn teste_erro_ao_inserir_duplicado() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        let result = repo.base.insert(&chapter_template()).await;
        assert!(matches!(result, Err(DbError::UniqueViolation)));
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let repo = setup().await;
        let result = repo.base.update(&chapter_template()).await;
        assert!(matches!(result, Err(DbError::NotFound)));
    }
}
