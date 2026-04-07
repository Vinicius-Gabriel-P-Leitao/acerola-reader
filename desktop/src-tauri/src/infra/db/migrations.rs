use tauri_plugin_sql::{ Migration, MigrationKind };

/// O projeto tem a propria ddl em rust com sqlx, mas ainda é usável pela config para registrar e versionar as migrations.
pub fn get_migrations() -> Vec<Migration> {
    vec![
        Migration {
            version: 1,
            description: "create_comic_templates",
            sql: include_str!("../../../migrations/archive/001_create_chapter_template.sql"),
            kind: MigrationKind::Up,
        },
        Migration {
            version: 2,
            description: "create_comic_directory",
            sql: include_str!("../../../migrations/archive/002_create_comic_directory.sql"),
            kind: MigrationKind::Up,
        },
        Migration {
            version: 3,
            description: "create_chapter_archive",
            sql: include_str!("../../../migrations/archive/003_create_chapter_archive.sql"),
            kind: MigrationKind::Up,
        }
    ]
}
