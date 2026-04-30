package br.acerola.comic.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.acerola.comic.local.converter.AcerolaTypeConverters
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ChapterTemplateDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.dao.category.CategoryDao
import br.acerola.comic.local.dao.history.ReadingHistoryDao
import br.acerola.comic.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.comic.local.dao.metadata.ChapterMetadataDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.BannerDao
import br.acerola.comic.local.dao.metadata.relationship.CoverDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.dao.metadata.source.AnilistSourceDao
import br.acerola.comic.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.comic.local.dao.metadata.source.MangadexSourceDao
import br.acerola.comic.local.dao.view.ComicSummaryDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ChapterTemplate
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.local.entity.archive.VolumeArchive
import br.acerola.comic.local.entity.category.Category
import br.acerola.comic.local.entity.category.ComicCategory
import br.acerola.comic.local.entity.history.ChapterRead
import br.acerola.comic.local.entity.history.ReadingHistory
import br.acerola.comic.local.entity.metadata.ChapterDownloadSource
import br.acerola.comic.local.entity.metadata.ChapterMetadata
import br.acerola.comic.local.entity.metadata.ComicMetadata
import br.acerola.comic.local.entity.metadata.relationship.Author
import br.acerola.comic.local.entity.metadata.relationship.Banner
import br.acerola.comic.local.entity.metadata.relationship.Cover
import br.acerola.comic.local.entity.metadata.relationship.Genre
import br.acerola.comic.local.entity.metadata.source.AnilistSource
import br.acerola.comic.local.entity.metadata.source.ComicInfoSource
import br.acerola.comic.local.entity.metadata.source.MangadexSource
import br.acerola.comic.local.entity.view.ComicSummaryView

@Database(
    entities = [
        ComicDirectory::class,
        ChapterTemplate::class,
        ComicMetadata::class,
        ChapterArchive::class,
        VolumeArchive::class,
        ChapterMetadata::class,
        ChapterDownloadSource::class,
        Author::class,
        Genre::class,
        Cover::class,
        Banner::class,
        ReadingHistory::class,
        ChapterRead::class,
        MangadexSource::class,
        AnilistSource::class,
        ComicInfoSource::class,
        Category::class,
        ComicCategory::class,
    ],
    views = [
        ComicSummaryView::class,
    ],
    exportSchema = false,
    version = 2,
)
@TypeConverters(AcerolaTypeConverters::class)
abstract class AcerolaDatabase : RoomDatabase() {
    abstract fun chapterArchiveDao(): ChapterArchiveDao

    abstract fun volumeArchiveDao(): VolumeArchiveDao

    abstract fun comicDirectoryDao(): ComicDirectoryDao

    abstract fun chapterTemplateDao(): ChapterTemplateDao

    abstract fun comicRemoteInfoDao(): ComicMetadataDao

    abstract fun chapterRemoteInfoDao(): ChapterMetadataDao

    abstract fun chapterDownloadSourceDao(): ChapterDownloadSourceDao

    abstract fun authorDao(): AuthorDao

    abstract fun coverDao(): CoverDao

    abstract fun bannerDao(): BannerDao

    abstract fun genreDao(): GenreDao

    abstract fun readingHistoryDao(): ReadingHistoryDao

    abstract fun mangadexSourceDao(): MangadexSourceDao

    abstract fun anilistSourceDao(): AnilistSourceDao

    abstract fun comicInfoSourceDao(): ComicInfoSourceDao

    abstract fun categoryDao(): CategoryDao

    abstract fun comicSummaryDao(): ComicSummaryDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 1. volume_archive
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

                    // 2. chapter_archive updates
                    db.execSQL("ALTER TABLE `chapter_archive` ADD COLUMN `is_special` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `chapter_archive` ADD COLUMN `volume_id_fk` INTEGER")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapter_archive_volume_id_fk` ON `chapter_archive` (`volume_id_fk`)")

                    // 3. chapter_read refactor
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
                    db.execSQL("DROP TABLE chapter_read")
                    db.execSQL("ALTER TABLE chapter_read_new RENAME TO chapter_read")

                    // 4. reading_history refactor
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
                    db.execSQL("DROP TABLE reading_history")
                    db.execSQL("ALTER TABLE reading_history_new RENAME TO reading_history")
                }
            }
    }
}
