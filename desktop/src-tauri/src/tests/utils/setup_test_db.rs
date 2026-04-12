pub async fn setup_test_db() -> sqlx::SqlitePool {
    let pool = sqlx::SqlitePool::connect("sqlite::memory:").await.unwrap();

    // archive
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

    // metadata
    sqlx::query(include_str!(
        "../../../migrations/models/metadata/001_create_comic_metadata.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/002_create_chapter_metadata.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/003_create_chapter_page.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/source/001_create_anilist_source.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/source/002_create_comic_info_source.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/source/003_create_mangadex_source.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/relationship/001_create_genre.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/relationship/002_create_cover.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/relationship/003_create_banner.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/metadata/relationship/004_create_author.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    // category
    sqlx::query(include_str!(
        "../../../migrations/models/category/001_create_category.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/category/002_create_manga_category.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    // history
    sqlx::query(include_str!(
        "../../../migrations/models/history/001_create_reading_history.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    sqlx::query(include_str!(
        "../../../migrations/models/history/002_create_chapter_read.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    // views
    sqlx::query(include_str!(
        "../../../migrations/views/001_create_comic_summary_view.sql"
    ))
    .execute(&pool)
    .await
    .unwrap();

    // seeds
    sqlx::query(include_str!(
        "../../../migrations/seeds/001_seed_chapter_template.sql"
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
