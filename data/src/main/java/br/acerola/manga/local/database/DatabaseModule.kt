@file:Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")

package br.acerola.manga.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.ChapterTemplateDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.category.CategoryDao
import br.acerola.manga.local.dao.history.ReadingHistoryDao
import br.acerola.manga.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.dao.metadata.ChapterMetadataDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.BannerDao
import br.acerola.manga.local.dao.metadata.relationship.CoverDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.AnilistSourceDao
import br.acerola.manga.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.manga.local.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.pattern.ChapterTemplatePattern
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
    fun provideDatabase(@ApplicationContext context: Context): AcerolaDatabase {
        return Room.databaseBuilder(
            context, AcerolaDatabase::class.java, "acerola_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                ChapterTemplatePattern.presets.entries.forEachIndexed { index, (label, pattern) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO chapter_template (id, label, pattern, is_default, priority) VALUES (?, ?, ?, 1, 0)",
                        arrayOf(-(index + 1).toLong(), label, pattern)
                    )
                }
            }
        })
        .build()
    }

    @Provides
    fun provideChapterArchiveDao(db: AcerolaDatabase): ChapterArchiveDao = db.chapterArchiveDao()

    @Provides
    fun provideMangaDirectoryDao(db: AcerolaDatabase): MangaDirectoryDao = db.mangaDirectoryDao()

    @Provides
    fun provideChapterTemplateDao(db: AcerolaDatabase): ChapterTemplateDao = db.chapterTemplateDao()

    @Provides
    fun provideMangaRemoteInfoDao(db: AcerolaDatabase): MangaMetadataDao = db.mangaRemoteInfoDao()

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
    fun provideMangaSummaryDao(db: AcerolaDatabase): br.acerola.manga.local.dao.view.MangaSummaryDao = db.mangaSummaryDao()
}
