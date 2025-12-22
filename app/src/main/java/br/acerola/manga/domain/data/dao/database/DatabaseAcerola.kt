package br.acerola.manga.domain.data.dao.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.acerola.manga.domain.data.converter.Converters
import br.acerola.manga.domain.data.dao.database.archive.ChapterFileDao
import br.acerola.manga.domain.data.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.data.dao.database.metadata.ChapterMetadataDao
import br.acerola.manga.domain.data.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.data.dao.database.metadata.author.AuthorDao
import br.acerola.manga.domain.data.dao.database.metadata.cover.CoverDao
import br.acerola.manga.domain.data.dao.database.metadata.gender.GenderDao
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.model.metadata.ChapterMetadata
import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.domain.model.metadata.author.Author
import br.acerola.manga.domain.model.metadata.cover.Cover
import br.acerola.manga.domain.model.metadata.gender.Gender

@Database(
    entities = [
        MangaFolder::class, ChapterFile::class, MangaMetadata::class,
        ChapterMetadata::class, Author::class, Gender::class, Cover::class
    ],
    exportSchema = false,
    version = 1,
)
@TypeConverters(Converters::class)
abstract class DatabaseAcerola : RoomDatabase() {
    abstract fun chapterFileDao(): ChapterFileDao
    abstract fun chapterMetadataDao(): ChapterMetadataDao
    abstract fun mangaFolderDao(): MangaFolderDao
    abstract fun mangaMetadataDao(): MangaMetadataDao
    abstract fun authorDao(): AuthorDao
    abstract fun coverDao(): CoverDao
    abstract fun genderDao(): GenderDao
}