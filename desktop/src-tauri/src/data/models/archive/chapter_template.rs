use serde::{ Deserialize, Serialize };

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ChapterTemplate {
    pub id: i64,
    pub label: String,
    pub pattern: String,
    pub is_default: bool,
    pub priority: i32,
}

#[cfg(test)]
mod tests {
    use crate::tests::utils::setup_test_db::setup_test_db;
    use super::ChapterTemplate;
    use sqlx::Row;

    #[tokio::test]
    async fn test_chapter_template_mapping() {
        let pool = setup_test_db().await;

        let template = ChapterTemplate {
            id: 1,
            label: "Default Preset".to_string(),
            pattern: "Chapter %d".to_string(),
            is_default: true,
            priority: 10,
        };
        sqlx::query(
            "INSERT INTO chapter_template (id, label, pattern, is_default, priority) VALUES (?, ?, ?, ?, ?)"
        )
            .bind(template.id)
            .bind(&template.label)
            .bind(&template.pattern)
            .bind(template.is_default)
            .bind(template.priority)
            .execute(&pool).await
            .unwrap();
        let row = sqlx
            ::query("SELECT * FROM chapter_template WHERE id = 1")
            .fetch_one(&pool).await
            .unwrap();
        assert_eq!(row.get::<String, _>("label"), "Default Preset");
        assert_eq!(row.get::<i32, _>("priority"), 10);
    }
}
