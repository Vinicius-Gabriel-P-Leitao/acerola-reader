use sqlx::{ Sqlite, sqlite::SqliteArguments, query::Query };
use crate::data::repositories::base::{ Entity, Bindable };
use serde::{ Deserialize, Serialize };

/// Contrato com o [`crate::data::repositories::base::Repository`] genérico.
impl Entity for ComicDirectory {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "name",
            "path",
            "cover",
            "banner",
            "last_modified",
            "chapter_template_fk",
            "external_sync_enabled",
            "hidden",
        ]
    }
    fn table_name() -> &'static str {
        "comic_directory"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ComicDirectory {
    fn bind_insert<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.name)
            .bind(&self.path)
            .bind(&self.cover)
            .bind(&self.banner)
            .bind(self.last_modified)
            .bind(self.chapter_template_fk)
            .bind(self.external_sync_enabled)
            .bind(self.hidden)
    }

    fn bind_update<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.name)
            .bind(&self.path)
            .bind(&self.cover)
            .bind(&self.banner)
            .bind(self.last_modified)
            .bind(self.chapter_template_fk)
            .bind(self.external_sync_enabled)
            .bind(self.hidden)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

/// Diretório de quadrinhos gerenciado pela aplicação.                                                                                                                                                                                                                                      ///
/// Equivalente a um `@Entity` do JPA — mapeia para a tabela `comic_directory`.
/// Migration em `src-tauri/migrations/archive`.
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ComicDirectory {
    pub id: i64,
    pub name: String,
    pub path: String,
    pub cover: Option<String>,
    pub banner: Option<String>,
    pub last_modified: i64,
    pub chapter_template_fk: Option<i64>,
    pub external_sync_enabled: bool,
    pub hidden: bool,
}

#[cfg(test)]
mod tests {
    use crate::tests::utils::setup_test_db::setup_test_db;
    use crate::data::repositories::base::Repository;
    use super::ComicDirectory;

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

    #[tokio::test]
    async fn test_insert_and_find_all() {
        let pool = setup_test_db().await;
        let repo = Repository::<ComicDirectory>::new(pool);

        let inserted = repo.insert(&berserk()).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.name, "Berserk");

        let all = repo.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn test_update() {
        let pool = setup_test_db().await;
        let repo = Repository::<ComicDirectory>::new(pool);

        repo.insert(&berserk()).await.unwrap();

        let updated = ComicDirectory { name: "Berserk Deluxe".to_string(), ..berserk() };
        let result = repo.update(&updated).await.unwrap();

        assert_eq!(result.name, "Berserk Deluxe");
    }

    #[tokio::test]
    async fn test_delete() {
        let pool = setup_test_db().await;
        let repo = Repository::<ComicDirectory>::new(pool);

        repo.insert(&berserk()).await.unwrap();
        repo.delete(1).await.unwrap();

        let all = repo.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }
}
