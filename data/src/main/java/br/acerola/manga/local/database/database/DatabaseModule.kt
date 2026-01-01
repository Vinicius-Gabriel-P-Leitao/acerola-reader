package br.acerola.manga.local.database.database

import android.content.Context
import androidx.room.Room
import br.acerola.manga.local.database.dao.archive.ChapterFileDao
import br.acerola.manga.local.database.dao.archive.MangaFolderDao
import br.acerola.manga.local.database.dao.metadata.ChapterMetadataDao
import br.acerola.manga.local.database.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.cover.CoverDao
import br.acerola.manga.local.database.dao.metadata.gender.GenderDao
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
    fun provideAuthorDao(db: DatabaseAcerola): AuthorDao = db.authorDao()

    @Provides
    fun provideGenderDao(db: DatabaseAcerola): GenderDao = db.genderDao()

    @Provides
    fun provideMangaFolderDao(db: DatabaseAcerola): MangaFolderDao = db.mangaFolderDao()

    @Provides
    fun provideMangaMetadataDao(db: DatabaseAcerola): MangaMetadataDao = db.mangaMetadataDao()

    @Provides
    fun provideChapterFileDao(db: DatabaseAcerola): ChapterFileDao = db.chapterFileDao()

    @Provides
    fun provideChapterMetadataDao(db: DatabaseAcerola): ChapterMetadataDao = db.chapterMetadataDao()

    @Provides
    fun provideCoverDao(db: DatabaseAcerola): CoverDao = db.coverDao()
}