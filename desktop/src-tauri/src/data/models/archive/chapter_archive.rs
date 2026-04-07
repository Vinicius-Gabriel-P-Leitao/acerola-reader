use sqlx::{ Sqlite, sqlite::SqliteArguments, query::Query };
use crate::data::repositories::base::{ Entity, Bindable };
use serde::{ Deserialize, Serialize };

/// Contrato com o [`crate::data::repositories::base::Repository`] genérico.
impl Entity for ChapterArchive {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "chapter",
            "path",
            "chapter_sort",
            "checksum",
            "fast_hash",
            "comic_directory_fk",
            "last_modified",
        ]
    }
    fn table_name() -> &'static str {
        "chapter_archive"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ChapterArchive {
    fn bind_insert<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.chapter)
            .bind(&self.path)
            .bind(&self.chapter_sort)
            .bind(&self.checksum)
            .bind(&self.fast_hash)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
    }

    fn bind_update<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.chapter)
            .bind(&self.path)
            .bind(&self.chapter_sort)
            .bind(&self.checksum)
            .bind(&self.fast_hash)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterArchive {
    pub id: i64,
    pub chapter: String,
    pub path: String,
    pub chapter_sort: String,
    pub checksum: Option<String>,
    pub fast_hash: Option<String>,
    pub comic_directory_fk: i64,
    pub last_modified: i64,
}

#[cfg(test)]
mod tests {
    use crate::data::models::archive::comic_directory::ComicDirectory;
    use crate::tests::utils::setup_test_db::setup_test_db;
    use crate::data::repositories::base::Repository;
    use super::ChapterArchive;

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

    fn berserk_chapter() -> ChapterArchive {
        ChapterArchive {
            id: 1,
            chapter: "Capítulo 1".to_string(),
            path: "/quadrinhos/berserk/cap1".to_string(),
            chapter_sort: "001".to_string(),
            checksum: Some("abc12345".to_string()),
            fast_hash: None,
            comic_directory_fk: berserk().id,
            last_modified: 123456789i64,
        }
    }

    async fn setup() -> sqlx::SqlitePool {
        let pool = setup_test_db().await;

        let repo = Repository::<ComicDirectory>::new(pool.clone());
        repo.insert(&berserk()).await.unwrap();

        pool
    }

    #[tokio::test]
    async fn test_insert_and_find_all() {
        let pool: sqlx::Pool<sqlx::Sqlite> = setup().await; // pool já tem o ComicDirectory
        let repo = Repository::<ChapterArchive>::new(pool);

        let inserted = repo.insert(&berserk_chapter()).await.unwrap();

        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.chapter, "Capítulo 1");

        let all = repo.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn test_update() {
        let pool = setup().await;
        let repo = Repository::<ChapterArchive>::new(pool);

        repo.insert(&berserk_chapter()).await.unwrap();

        let updated = ChapterArchive { chapter: "Capítulo 2".to_string(), ..berserk_chapter() };
        let result = repo.update(&updated).await.unwrap();

        assert_eq!(result.chapter, "Capítulo 2");
    }

    #[tokio::test]
    async fn test_delete() {
        let pool = setup().await;
        let repo = Repository::<ChapterArchive>::new(pool);

        repo.insert(&berserk_chapter()).await.unwrap();
        repo.delete(1).await.unwrap();

        let all = repo.find_all().await.unwrap();
        assert_eq!(all.len(), 0);
    }
}
