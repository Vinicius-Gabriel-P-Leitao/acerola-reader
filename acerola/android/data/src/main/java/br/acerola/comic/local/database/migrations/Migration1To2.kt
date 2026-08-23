package br.acerola.comic.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. archive_template (replaces chapter_template)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `archive_template` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `label` TEXT NOT NULL,
                    `pattern` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `is_default` INTEGER NOT NULL DEFAULT 0,
                    `priority` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_archive_template_label` ON `archive_template` (`label`)",
            )
            db.execSQL(
                """
                INSERT INTO `archive_template` (`id`, `label`, `pattern`, `type`, `is_default`, `priority`)
                SELECT `id`, `label`, `pattern`, 'CHAPTER', `is_default`, `priority`
                FROM `chapter_template`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `chapter_template`")

            // 2. comic_directory (rename chapter_template_fk → archive_template_fk)
            db.execSQL(
                """
                CREATE TABLE `comic_directory_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `cover` TEXT,
                    `banner` TEXT,
                    `last_modified` INTEGER NOT NULL,
                    `archive_template_fk` INTEGER,
                    `external_sync_enabled` INTEGER NOT NULL DEFAULT 1,
                    `hidden` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`archive_template_fk`) REFERENCES `archive_template`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `comic_directory_new` (`id`, `name`, `path`, `cover`, `banner`, `last_modified`, `archive_template_fk`, `external_sync_enabled`, `hidden`)
                SELECT `id`, `name`, `path`, `cover`, `banner`, `last_modified`, `chapter_template_fk`, `external_sync_enabled`, `hidden`
                FROM `comic_directory`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `comic_directory`")
            db.execSQL("ALTER TABLE `comic_directory_new` RENAME TO `comic_directory`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_comic_directory_name` ON `comic_directory` (`name`)",
            )

            // 3. volume_archive
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `volume_archive` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `volume_sort` TEXT NOT NULL,
                    `is_special` INTEGER NOT NULL DEFAULT 0,
                    `cover` TEXT,
                    `banner` TEXT,
                    `comic_directory_fk` INTEGER NOT NULL,
                    `last_modified` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`comic_directory_fk`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_volume_archive_comic_directory_fk_volume_sort` ON `volume_archive` (`comic_directory_fk`, `volume_sort`)",
            )

            // 4. chapter_archive (recreate to add volume_id_fk FK — ALTER TABLE doesn't support FK declarations)
            db.execSQL(
                """
                CREATE TABLE `chapter_archive_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `chapter` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `chapter_sort` TEXT NOT NULL,
                    `is_special` INTEGER NOT NULL DEFAULT 0,
                    `checksum` TEXT,
                    `fast_hash` TEXT,
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
                INSERT INTO `chapter_archive_new` (`id`, `chapter`, `path`, `chapter_sort`, `checksum`, `fast_hash`, `comic_directory_fk`, `last_modified`)
                SELECT `id`, `chapter`, `path`, `chapter_sort`, `checksum`, `fast_hash`, `comic_directory_fk`, `last_modified`
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

            // 5. chapter_read refactor
            db.execSQL(
                """
                CREATE TABLE `chapter_read_new` (
                    `comic_directory_id` INTEGER NOT NULL,
                    `chapter_sort` TEXT NOT NULL,
                    `chapter_archive_id` INTEGER,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`comic_directory_id`, `chapter_sort`),
                    FOREIGN KEY(`comic_directory_id`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `chapter_read_new` (comic_directory_id, chapter_sort, chapter_archive_id, created_at)
                SELECT cr.comic_directory_id, ca.chapter_sort, cr.chapter_archive_id, cr.created_at
                FROM chapter_read cr
                JOIN chapter_archive ca ON cr.chapter_archive_id = ca.id
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `chapter_read`")
            db.execSQL("ALTER TABLE `chapter_read_new` RENAME TO `chapter_read`")

            // 6. reading_history refactor
            db.execSQL(
                """
                CREATE TABLE `reading_history_new` (
                    `comic_directory_id` INTEGER NOT NULL,
                    `chapter_sort` TEXT NOT NULL,
                    `chapter_archive_id` INTEGER,
                    `last_page` INTEGER NOT NULL,
                    `is_completed` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`comic_directory_id`),
                    FOREIGN KEY(`comic_directory_id`) REFERENCES `comic_directory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `reading_history_new` (comic_directory_id, chapter_sort, chapter_archive_id, last_page, is_completed, updated_at)
                SELECT rh.comic_directory_id, ca.chapter_sort, rh.chapter_archive_id, rh.last_page, rh.is_completed, rh.updated_at
                FROM reading_history rh
                JOIN chapter_archive ca ON rh.chapter_archive_id = ca.id
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `reading_history`")
            db.execSQL("ALTER TABLE `reading_history_new` RENAME TO `reading_history`")

            // 7. manga_category → comic_category (tableName was renamed without version bump)
            db.execSQL("ALTER TABLE `manga_category` RENAME TO `comic_category`")
            db.execSQL("DROP INDEX IF EXISTS `index_manga_category_comic_directory_fk`")
            db.execSQL("DROP INDEX IF EXISTS `index_manga_category_category_id`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_comic_category_comic_directory_fk` ON `comic_category` (`comic_directory_fk`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_comic_category_category_id` ON `comic_category` (`category_id`)",
            )
        }
    }
