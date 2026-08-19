use sqlx::SqlitePool;

use crate::data::{models::archive::archive_template::ArchiveTemplate, repositories::Repository};

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
    use crate::{
        data::models::archive::archive_template::{ArchiveTemplate, SortType},
        infra::error::DbError,
        tests::utils::setup_test_db::setup_test_db,
    };

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
    async fn test_insert_chapter_template() {
        let repo = setup().await;
        let inserted = repo.base.insert(&chapter_template()).await.unwrap();
        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.sort_type, SortType::Chapter);
    }

    #[tokio::test]
    async fn test_insert_volume_template() {
        let repo = setup().await;
        let inserted = repo.base.insert(&volume_template()).await.unwrap();
        assert_eq!(inserted.sort_type, SortType::Volume);
    }

    #[tokio::test]
    async fn test_find_all() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        repo.base.insert(&volume_template()).await.unwrap();
        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 2);
    }

    #[tokio::test]
    async fn test_update() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        let updated = ArchiveTemplate { label: "Updated".to_string(), ..chapter_template() };
        let result = repo.base.update(&updated).await.unwrap();
        assert_eq!(result.label, "Updated");
    }

    #[tokio::test]
    async fn test_delete() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        repo.base.delete(1).await.unwrap();
        assert_eq!(repo.base.find_all().await.unwrap().len(), 0);
    }

    #[tokio::test]
    async fn test_error_on_duplicate_insert() {
        let repo = setup().await;
        repo.base.insert(&chapter_template()).await.unwrap();
        let result = repo.base.insert(&chapter_template()).await;
        assert!(matches!(result, Err(DbError::UniqueViolation)));
    }

    #[tokio::test]
    async fn test_error_on_updating_nonexistent() {
        let repo = setup().await;
        let result = repo.base.update(&chapter_template()).await;
        assert!(matches!(result, Err(DbError::NotFound)));
    }
}
