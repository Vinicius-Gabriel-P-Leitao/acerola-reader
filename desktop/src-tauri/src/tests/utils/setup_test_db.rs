pub async fn setup_test_db() -> sqlx::SqlitePool {
    let pool = sqlx::SqlitePool::connect("sqlite::memory:").await.unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/archive/001_create_chapter_template.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/archive/002_create_comic_directory.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/archive/003_create_chapter_archive.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    pool
}
