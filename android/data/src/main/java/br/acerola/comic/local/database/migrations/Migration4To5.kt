package br.acerola.comic.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_history_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `peer_id` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `message` TEXT,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_history_log_created_at` ON `sync_history_log` (`created_at`)",
            )
        }
    }
