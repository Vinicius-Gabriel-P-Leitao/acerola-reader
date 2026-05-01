@file:Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")

package br.acerola.comic.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.acerola.comic.local.dao.archive.ArchiveTemplateDao
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
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
import br.acerola.comic.pattern.template.TemplateMacro
import br.acerola.comic.util.sort.SortType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AcerolaDatabase =
        Room
            .databaseBuilder(
                context,
                AcerolaDatabase::class.java,
                "acerola_database",
            ).addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val chapterPresets =
                            mapOf(
                                "01.*." to
                                    "{${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                                    ".*.{${TemplateMacro.EXTENSION.tag}}",
                                "Ch. 01.*." to
                                    "Ch. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                                    ".*.{${TemplateMacro.EXTENSION.tag}}",
                                "Cap. 01.*." to
                                    "Cap. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                                    ".*.{${TemplateMacro.EXTENSION.tag}}",
                                "chapter 01.*." to
                                    "chapter {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                                    ".*.{${TemplateMacro.EXTENSION.tag}}",
                            )

                        val volumePresets =
                            mapOf(
                                "Vol. 01" to "Vol. {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
                                "Volume 01" to "Volume {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
                                "V01" to "V{${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
                                "Edicao 01" to "Edicao {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
                                "Edição 01" to "Edição {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
                            )

                        var index = 1L
                        chapterPresets.forEach { (label, pattern) ->
                            db.execSQL(
                                "INSERT OR IGNORE INTO archive_template (id, label, pattern, type, is_default, priority) VALUES (?, ?, ?, ?, 1, 0)",
                                arrayOf(-index, label, pattern, SortType.CHAPTER.name),
                            )
                            index++
                        }

                        volumePresets.forEach { (label, pattern) ->
                            db.execSQL(
                                "INSERT OR IGNORE INTO archive_template (id, label, pattern, type, is_default, priority) VALUES (?, ?, ?, ?, 1, 0)",
                                arrayOf(-index, label, pattern, SortType.VOLUME.name),
                            )
                            index++
                        }
                    }
                },
            ).addMigrations(AcerolaDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()

    @Provides
    fun provideChapterArchiveDao(db: AcerolaDatabase): ChapterArchiveDao = db.chapterArchiveDao()

    @Provides
    fun provideVolumeArchiveDao(db: AcerolaDatabase): VolumeArchiveDao = db.volumeArchiveDao()

    @Provides
    fun provideMangaDirectoryDao(db: AcerolaDatabase): ComicDirectoryDao = db.comicDirectoryDao()

    @Provides
    fun provideArchiveTemplateDao(db: AcerolaDatabase): ArchiveTemplateDao = db.archiveTemplateDao()

    @Provides
    fun provideMangaRemoteInfoDao(db: AcerolaDatabase): ComicMetadataDao = db.comicRemoteInfoDao()

    @Provides
    fun provideChapterRemoteInfoDao(db: AcerolaDatabase): ChapterMetadataDao = db.chapterRemoteInfoDao()

    @Provides
    fun provideChapterDownloadSourceDao(db: AcerolaDatabase): ChapterDownloadSourceDao = db.chapterDownloadSourceDao()

    @Provides
    fun provideAuthorDao(db: AcerolaDatabase): AuthorDao = db.authorDao()

    @Provides
    fun provideCoverDao(db: AcerolaDatabase): CoverDao = db.coverDao()

    @Provides
    fun provideBannerDao(db: AcerolaDatabase): BannerDao = db.bannerDao()

    @Provides
    fun provideGenreDao(db: AcerolaDatabase): GenreDao = db.genreDao()

    @Provides
    fun provideReadingHistoryDao(db: AcerolaDatabase): ReadingHistoryDao = db.readingHistoryDao()

    @Provides
    fun provideMangadexSourceDao(db: AcerolaDatabase): MangadexSourceDao = db.mangadexSourceDao()

    @Provides
    fun provideAnilistSourceDao(db: AcerolaDatabase): AnilistSourceDao = db.anilistSourceDao()

    @Provides
    fun provideComicInfoSourceDao(db: AcerolaDatabase): ComicInfoSourceDao = db.comicInfoSourceDao()

    @Provides
    fun provideCategoryDao(db: AcerolaDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideMangaSummaryDao(db: AcerolaDatabase): ComicSummaryDao = db.comicSummaryDao()
}
