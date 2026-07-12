use sqlx::SqlitePool;

use crate::{
    data::{
        models::metadata::{author::AuthorMetadata, chapter::ChapterMetadata, comic::ComicMetadata, page::ChapterPage, cover::Cover, banner::Banner},
        repositories::{Entity, Repository},
    },
    infra::error::DbError,
};

#[derive(Clone)]
pub struct MetadataRepository {
    pub comic_repo: Repository<ComicMetadata>,
    pub chapter_repo: Repository<ChapterMetadata>,
    pub page_repo: Repository<ChapterPage>,
    pub author_repo: Repository<AuthorMetadata>,
    pub cover_repo: Repository<Cover>,
    pub banner_repo: Repository<Banner>,
    pool: SqlitePool,
}

impl MetadataRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            comic_repo: Repository::new(pool.clone()),
            chapter_repo: Repository::new(pool.clone()),
            page_repo: Repository::new(pool.clone()),
            author_repo: Repository::new(pool.clone()),
            cover_repo: Repository::new(pool.clone()),
            banner_repo: Repository::new(pool.clone()),
            pool,
        }
    }

    pub async fn get_comic_metadata_by_comic_id(&self, comic_id: i64) -> Result<Option<ComicMetadata>, DbError> {
        let table = ComicMetadata::table_name();
        let cols = ComicMetadata::columns().join(", ");

        let result = sqlx::query_as::<_, ComicMetadata>(&format!(
            "SELECT {} FROM {} WHERE comic_directory_fk = ?",
            cols, table
        ))
        .bind(comic_id)
        .fetch_optional(&self.pool)
        .await?;

        Ok(result)
    }

    pub async fn get_chapter_metadata_by_comic_metadata_id(&self, metadata_id: i64) -> Result<Vec<ChapterMetadata>, DbError> {
        let table = ChapterMetadata::table_name();
        let cols = ChapterMetadata::columns().join(", ");

        let result = sqlx::query_as::<_, ChapterMetadata>(&format!(
            "SELECT {} FROM {} WHERE comic_metadata_fk = ?",
            cols, table
        ))
        .bind(metadata_id)
        .fetch_all(&self.pool)
        .await?;
        Ok(result)
    }

    pub async fn get_author_metadata_by_comic_metadata_id(&self, metadata_id: i64) -> Result<Vec<AuthorMetadata>, DbError> {
        let table = AuthorMetadata::table_name();
        let cols = AuthorMetadata::columns().join(", ");

        let result = sqlx::query_as::<_, AuthorMetadata>(&format!(
            "SELECT {} FROM {} WHERE comic_metadata_fk = ?",
            cols, table
        ))
        .bind(metadata_id)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }

    pub async fn get_cover_by_comic_metadata_id(&self, metadata_id: i64) -> Result<Option<Cover>, DbError> {
        let table = Cover::table_name();
        let cols = Cover::columns().join(", ");

        let result = sqlx::query_as::<_, Cover>(&format!(
            "SELECT {} FROM {} WHERE comic_metadata_fk = ?",
            cols, table
        ))
        .bind(metadata_id)
        .fetch_optional(&self.pool)
        .await?;

        Ok(result)
    }

    pub async fn get_banner_by_comic_metadata_id(&self, metadata_id: i64) -> Result<Option<Banner>, DbError> {
        let table = Banner::table_name();
        let cols = Banner::columns().join(", ");

        let result = sqlx::query_as::<_, Banner>(&format!(
            "SELECT {} FROM {} WHERE comic_metadata_fk = ?",
            cols, table
        ))
        .bind(metadata_id)
        .fetch_optional(&self.pool)
        .await?;

        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::utils::setup_test_db::setup_test_db;

    async fn setup() -> MetadataRepository {
        MetadataRepository::new(setup_test_db().await)
    }

    fn fake_comic_metadata() -> ComicMetadata {
        ComicMetadata {
            id: 1,
            title: "Berserk".to_string(),
            description: "Dark fantasy".to_string(),
            romanji: "Berserk".to_string(),
            status: "Ongoing".to_string(),
            publication: Some(1989),
            sync_source: Some("MangaDex".to_string()),
            has_comic_info: false,
            comic_directory_fk: None,
        }
    }

    fn fake_chapter_metadata() -> ChapterMetadata {
        ChapterMetadata {
            id: 1,
            title: Some("The Black Swordsman".to_string()),
            chapter: "1".to_string(),
            page_count: Some(50),
            scanlation: Some("Evil Genius".to_string()),
            comic_metadata_fk: 1,
        }
    }

    #[tokio::test]
    async fn deve_inserir_e_buscar_comic_metadata() {
        let repo = setup().await;
        
        let metadata = fake_comic_metadata();
        
        let inserted = repo.comic_repo.insert(&metadata).await.unwrap();
        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.title, "Berserk");
        
        let all = repo.comic_repo.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn deve_buscar_comic_metadata_pelo_id() {
        let repo = setup().await;
        
        let mut metadata = fake_comic_metadata();
        metadata.comic_directory_fk = Some(999);
        repo.comic_repo.insert(&metadata).await.unwrap();
        
        let result = repo.get_comic_metadata_by_comic_id(999).await.unwrap();
        assert!(result.is_some());
        assert_eq!(result.unwrap().title, "Berserk");
        
        let not_found = repo.get_comic_metadata_by_comic_id(123).await.unwrap();
        assert!(not_found.is_none());
    }

    #[tokio::test]
    async fn deve_inserir_e_buscar_chapter_metadata() {
        let repo = setup().await;
        
        let metadata = fake_comic_metadata();
        repo.comic_repo.insert(&metadata).await.unwrap();
        
        let chapter = fake_chapter_metadata();
        let inserted = repo.chapter_repo.insert(&chapter).await.unwrap();
        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.chapter, "1");
        
        let chapters = repo.get_chapter_metadata_by_comic_metadata_id(1).await.unwrap();
        assert_eq!(chapters.len(), 1);
        assert_eq!(chapters[0].chapter, "1");
    }
}
