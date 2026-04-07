use sqlx::{ Sqlite, sqlite::SqliteArguments, query::Query };
use crate::data::repositories::base::{ Entity, Bindable };
use serde::{ Deserialize, Serialize };

/// Contrato com o [`crate::data::repositories::base::Repository`] genérico.
impl Entity for ChapterTemplate {
    fn columns() -> &'static [&'static str] {
        &["id", "label", "pattern", "is_default", "priority"]
    }
    fn table_name() -> &'static str {
        "chapter_template"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ChapterTemplate {
    fn bind_insert<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.label)
            .bind(&self.pattern)
            .bind(self.is_default)
            .bind(self.priority)
    }

    fn bind_update<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.label)
            .bind(&self.pattern)
            .bind(self.is_default)
            .bind(self.priority)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterTemplate {
    pub id: i64,
    pub label: String,
    pub pattern: String,
    pub is_default: bool,
    pub priority: i64,
}

#[cfg(test)]
mod tests {
    use crate::tests::utils::setup_test_db::setup_test_db;
    use crate::data::repositories::base::Repository;
    use super::ChapterTemplate;

    fn template() -> ChapterTemplate {
        ChapterTemplate {
            id: 1,
            label: "Default Preset".to_string(),
            pattern: "Chapter %d".to_string(),
            is_default: true,
            priority: 10,
        }
    }

    #[tokio::test]
    async fn test_insert_and_find_all() {
        let pool = setup_test_db().await;
        let repo = Repository::<ChapterTemplate>::new(pool);

        let inserted = repo.insert(&template()).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.label, "Default Preset");

        let all = repo.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn test_update() {
        let pool = setup_test_db().await;
        let repo = Repository::<ChapterTemplate>::new(pool);

        repo.insert(&template()).await.unwrap();

        let updated = ChapterTemplate { label: "Default Preset Deluxe".to_string(), ..template() };
        let result = repo.update(&updated).await.unwrap();

        assert_eq!(result.label, "Default Preset Deluxe");
    }

    #[tokio::test]
    async fn test_delete() {
        let pool = setup_test_db().await;
        let repo = Repository::<ChapterTemplate>::new(pool);

        repo.insert(&template()).await.unwrap();
        repo.delete(1).await.unwrap();

        let all = repo.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }
}
