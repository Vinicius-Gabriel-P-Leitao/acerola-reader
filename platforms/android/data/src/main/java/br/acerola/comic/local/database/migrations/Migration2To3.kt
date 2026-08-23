package br.acerola.comic.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Drop obsoleted tables
            db.execSQL("DROP TABLE IF EXISTS `chapter_page`")
            db.execSQL("DROP TABLE IF EXISTS `cover`")
            db.execSQL("DROP TABLE IF EXISTS `banner`")
            db.execSQL("DROP TABLE IF EXISTS `comic_info_source`")

            // 2. Refactor mangadex_source (remove store URLs and anilist_id)
            db.execSQL(
                """
                CREATE TABLE `mangadex_source_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `mangadex_id` TEXT NOT NULL,
                    `comic_metadata_fk` INTEGER NOT NULL,
                    FOREIGN KEY(`comic_metadata_fk`) REFERENCES `comic_metadata`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `mangadex_source_new` (`id`, `mangadex_id`, `comic_metadata_fk`)
                SELECT `id`, `mangadex_id`, `comic_metadata_fk`
                FROM `mangadex_source`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `mangadex_source`")
            db.execSQL("ALTER TABLE `mangadex_source_new` RENAME TO `mangadex_source`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_mangadex_source_comic_metadata_fk` ON `mangadex_source` (`comic_metadata_fk`)",
            )

            // 3. Refactor chapter_archive (remove fast_hash)
            db.execSQL(
                """
                CREATE TABLE `chapter_archive_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `chapter` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `chapter_sort` TEXT NOT NULL,
                    `is_special` INTEGER NOT NULL DEFAULT 0,
                    `checksum` TEXT,
                    `comic_directory_fk` INTEGER NOT NULL,
                    `volume_id_fk` INTEGER,
                    `last_modified` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`comic_directory_fk`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`volume_id_fk`) REFERENCES `volume_archive`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `chapter_archive_new` (`id`, `chapter`, `path`, `chapter_sort`, `is_special`, `checksum`, `comic_directory_fk`, `volume_id_fk`, `last_modified`)
                SELECT `id`, `chapter`, `path`, `chapter_sort`, `is_special`, `checksum`, `comic_directory_fk`, `volume_id_fk`, `last_modified`
                FROM `chapter_archive`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `chapter_archive`")
            db.execSQL("ALTER TABLE `chapter_archive_new` RENAME TO `chapter_archive`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_chapter_archive_comic_directory_fk_chapter` ON `chapter_archive` (`comic_directory_fk`, `chapter`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_chapter_archive_volume_id_fk` ON `chapter_archive` (`volume_id_fk`)",
            )

            // 4. Refactor comic_metadata (remove romanji)
            db.execSQL(
                """
                CREATE TABLE `comic_metadata_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `publication` INTEGER,
                    `sync_source` TEXT,
                    `has_comic_info` INTEGER NOT NULL,
                    `comic_directory_fk` INTEGER,
                    FOREIGN KEY(`comic_directory_fk`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `comic_metadata_new` (`id`, `title`, `description`, `status`, `publication`, `sync_source`, `has_comic_info`, `comic_directory_fk`)
                SELECT `id`, `title`, `description`, `status`, `publication`, `sync_source`, `has_comic_info`, `comic_directory_fk`
                FROM `comic_metadata`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `comic_metadata`")
            db.execSQL("ALTER TABLE `comic_metadata_new` RENAME TO `comic_metadata`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_comic_metadata_comic_directory_fk` ON `comic_metadata` (`comic_directory_fk`)",
            )

            // 5. Refactor chapter_metadata (add chapter_archive_fk)
            db.execSQL(
                """
                CREATE TABLE `chapter_metadata_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT,
                    `chapter` TEXT NOT NULL,
                    `page_count` INTEGER,
                    `scanlation` TEXT,
                    `comic_metadata_fk` INTEGER NOT NULL,
                    `chapter_archive_fk` INTEGER,
                    FOREIGN KEY(`comic_metadata_fk`) REFERENCES `comic_metadata`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`chapter_archive_fk`) REFERENCES `chapter_archive`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `chapter_metadata_new` (`id`, `title`, `chapter`, `page_count`, `scanlation`, `comic_metadata_fk`, `chapter_archive_fk`)
                SELECT `id`, `title`, `chapter`, `page_count`, `scanlation`, `comic_metadata_fk`, NULL
                FROM `chapter_metadata`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `chapter_metadata`")
            db.execSQL("ALTER TABLE `chapter_metadata_new` RENAME TO `chapter_metadata`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_chapter_metadata_comic_metadata_fk` ON `chapter_metadata` (`comic_metadata_fk`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_chapter_metadata_chapter_archive_fk` ON `chapter_metadata` (`chapter_archive_fk`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_chapter_metadata_chapter_comic_metadata_fk` ON `chapter_metadata` (`chapter`, `comic_metadata_fk`)",
            )

            // 6. Refactor comic_category (category_id -> category_fk)
            db.execSQL(
                """
                CREATE TABLE `comic_category_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `comic_directory_fk` INTEGER NOT NULL,
                    `category_fk` INTEGER NOT NULL,
                    FOREIGN KEY(`comic_directory_fk`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`category_fk`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `comic_category_new` (`id`, `comic_directory_fk`, `category_fk`)
                SELECT `id`, `comic_directory_fk`, `category_id`
                FROM `comic_category`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `comic_category`")
            db.execSQL("ALTER TABLE `comic_category_new` RENAME TO `comic_category`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_comic_category_comic_directory_fk` ON `comic_category` (`comic_directory_fk`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_comic_category_category_fk` ON `comic_category` (`category_fk`)",
            )

            // 7. Refactor chapter_read (comic_directory_id -> comic_directory_fk, chapter_archive_id -> chapter_archive_fk)
            db.execSQL(
                """
                CREATE TABLE `chapter_read_new` (
                    `comic_directory_fk` INTEGER NOT NULL,
                    `chapter_sort` TEXT NOT NULL,
                    `chapter_archive_fk` INTEGER,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`comic_directory_fk`, `chapter_sort`),
                    FOREIGN KEY(`comic_directory_fk`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `chapter_read_new` (`comic_directory_fk`, `chapter_sort`, `chapter_archive_fk`, `created_at`)
                SELECT `comic_directory_id`, `chapter_sort`, `chapter_archive_id`, `created_at`
                FROM `chapter_read`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `chapter_read`")
            db.execSQL("ALTER TABLE `chapter_read_new` RENAME TO `chapter_read`")

            // 8. Refactor reading_history (comic_directory_id -> comic_directory_fk, chapter_archive_id -> chapter_archive_fk)
            db.execSQL(
                """
                CREATE TABLE `reading_history_new` (
                    `comic_directory_fk` INTEGER NOT NULL,
                    `chapter_sort` TEXT NOT NULL,
                    `chapter_archive_fk` INTEGER,
                    `last_page` INTEGER NOT NULL,
                    `is_completed` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`comic_directory_fk`),
                    FOREIGN KEY(`comic_directory_fk`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `reading_history_new` (`comic_directory_fk`, `chapter_sort`, `chapter_archive_fk`, `last_page`, `is_completed`, `updated_at`)
                SELECT `comic_directory_id`, `chapter_sort`, `chapter_archive_id`, `last_page`, `is_completed`, `updated_at`
                FROM `reading_history`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `reading_history`")
            db.execSQL("ALTER TABLE `reading_history_new` RENAME TO `reading_history`")
        }
    }
