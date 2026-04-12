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

/// Pool com um comic_directory já inserido — usado por testes de chapter que precisam da FK.
pub async fn setup_test_db_with_comic() -> sqlx::SqlitePool {
    let pool = setup_test_db().await;

    sqlx::query(
        "INSERT INTO comic_directory (id, name, path, last_modified, external_sync_enabled, hidden)
         VALUES (1, 'Test', '/test', 0, 0, 0)",
    )
    .execute(&pool)
    .await
    .unwrap();

    pool
}
