use serde::{ Deserialize, Serialize };

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone)]
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
    use super::super::comic_directory::ComicDirectory;
    use sqlx::Row;

    #[tokio::test]
    async fn test_comic_directory_mapping() {
        // NOTE: Setup
        let pool = setup_test_db().await;

        let comic: ComicDirectory = ComicDirectory {
            id: 1,
            name: "Berserk".to_string(),
            path: "/quadrinhos/berserk".to_string(),
            cover: None,
            banner: None,
            last_modified: 1700000000,
            chapter_template_fk: None,
            external_sync_enabled: true,
            hidden: false,
        };

        // NOTE: Ação: Inserção direta (simulando o DAO)
        sqlx::query(
            "INSERT INTO comic_directory (id, name, path, last_modified, external_sync_enabled, hidden) VALUES (?, ?, ?, ?, ?, ?)"
        )
            .bind(comic.id)
            .bind(&comic.name)
            .bind(&comic.path)
            .bind(comic.last_modified)
            .bind(comic.external_sync_enabled)
            .bind(comic.hidden)
            .execute(&pool).await
            .unwrap();

        // NOTE: Validação: Ler de volta e checar se o mapeamento é real
        let row = sqlx
            ::query("SELECT * FROM comic_directory WHERE id = 1")
            .fetch_one(&pool).await
            .unwrap();

        assert_eq!(row.get::<String, _>("name"), "Berserk");
        assert_eq!(row.get::<i64, _>("id"), 1);
    }
}
