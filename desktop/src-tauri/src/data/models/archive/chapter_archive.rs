use serde::{ Deserialize, Serialize };

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone)]
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
    use crate::tests::utils::setup_test_db::setup_test_db;
    use super::ChapterArchive;
    use sqlx::Row;

    #[tokio::test]
    async fn test_chapter_archive_mapping() {
        let pool = setup_test_db().await;

        // NOTE: Setup, Inserir quadrinho pai primeiro para satisfazer a constraint de FOREIGN KEY
        sqlx::query(
            "INSERT INTO comic_directory (id, name, path, last_modified, external_sync_enabled, hidden) VALUES (?, ?, ?, ?, ?, ?)"
        )
            .bind(1i64)
            .bind("Berserk")
            .bind("/quadrinhos/berserk")
            .bind(1700000000i64)
            .bind(true)
            .bind(false)
            .execute(&pool).await
            .unwrap();

        // NOTE: Arrange, Criar a struct ChapterArchive
        let chapter = ChapterArchive {
            id: 1,
            chapter: "Capítulo 1".to_string(),
            path: "/quadrinhos/berserk/cap1".to_string(),
            chapter_sort: "001".to_string(),
            checksum: Some("abc12345".to_string()),
            fast_hash: None,
            comic_directory_fk: 1,
            last_modified: 123456789i64,
        };

        // NOTE: Act, Tentar inserir no banco
        sqlx::query(
            "INSERT INTO chapter_archive (id, chapter, chapter_path, chapter_sort, checksum, fast_hash, comic_directory_fk, last_modified) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        )
            .bind(chapter.id)
            .bind(&chapter.chapter)
            .bind(&chapter.path)
            .bind(&chapter.chapter_sort)
            .bind(&chapter.checksum)
            .bind(&chapter.fast_hash)
            .bind(chapter.comic_directory_fk)
            .bind(chapter.last_modified)
            .execute(&pool).await
            .unwrap();

        // NOTE: Assert, Validar mapeamento
        let row = sqlx
            ::query("SELECT * FROM chapter_archive WHERE id = 1")
            .fetch_one(&pool).await
            .unwrap();
        assert_eq!(row.get::<String, _>("chapter"), "Capítulo 1");
        assert_eq!(row.get::<i64, _>("comic_directory_fk"), 1);
    }
}
