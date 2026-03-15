package br.acerola.manga.local.database.database

import android.content.Context
import androidx.room.Room
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.history.ReadingHistoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.cover.CoverDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
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
    fun provideDatabase(@ApplicationContext context: Context): DatabaseAcerola {
        return Room.databaseBuilder(
            context, klass = DatabaseAcerola::class.java, name = "acerola_database"
        ).fallbackToDestructiveMigration(dropAllTables = false).build()
    }

    @Provides
    fun provideMangaDirectoryDao(db: DatabaseAcerola): MangaDirectoryDao = db.mangaDirectoryDao()

    @Provides
    fun provideMangaRemoteInfoDao(db: DatabaseAcerola): MangaRemoteInfoDao = db.mangaMangaRemoteInfoDao()

    @Provides
    fun provideChapterArchiveDao(db: DatabaseAcerola): ChapterArchiveDao = db.chapterArchiveDao()

    @Provides
    fun provideChapterRemoteInfoDao(db: DatabaseAcerola): ChapterRemoteInfoDao = db.chapterRemoteInfoDao()

    @Provides
    fun provideChapterDownloadSourceDao(db: DatabaseAcerola): ChapterDownloadSourceDao = db.chapterDownloadSourceDao()

    @Provides
    fun provideCoverDao(db: DatabaseAcerola): CoverDao = db.coverDao()

    @Provides
    fun provideAuthorDao(db: DatabaseAcerola): AuthorDao = db.authorDao()

    @Provides
    fun provideGenreDao(db: DatabaseAcerola): GenreDao = db.genreDao()

    @Provides
    fun provideReadingHistoryDao(db: DatabaseAcerola): ReadingHistoryDao = db.readingHistoryDao()
}