package br.acerola.manga.local.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.acerola.manga.local.converter.DatabaseConverters
import br.acerola.manga.local.database.dao.archive.ChapterFileDao
import br.acerola.manga.local.database.dao.archive.MangaFolderDao
import br.acerola.manga.local.database.dao.metadata.ChapterMetadataDao
import br.acerola.manga.local.database.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.cover.CoverDao
import br.acerola.manga.local.database.dao.metadata.gender.GenderDao
import br.acerola.manga.local.database.entity.archive.ChapterFile
import br.acerola.manga.local.database.entity.archive.MangaFolder
import br.acerola.manga.local.database.entity.metadata.ChapterMetadata
import br.acerola.manga.local.database.entity.metadata.ChapterMetadataFile
import br.acerola.manga.local.database.entity.metadata.MangaMetadata
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Gender

@Database(
    entities = [
        MangaFolder::class, MangaMetadata::class, ChapterMetadataFile::class, ChapterMetadata::class,
        ChapterFile::class, Author::class, Gender::class, Cover::class
    ],
    exportSchema = false,
    version = 1,
)
@TypeConverters(DatabaseConverters::class)
abstract class DatabaseAcerola : RoomDatabase() {
    abstract fun chapterFileDao(): ChapterFileDao
    abstract fun chapterMetadataDao(): ChapterMetadataDao
    abstract fun mangaFolderDao(): MangaFolderDao
    abstract fun mangaMetadataDao(): MangaMetadataDao
    abstract fun authorDao(): AuthorDao
    abstract fun coverDao(): CoverDao
    abstract fun genderDao(): GenderDao
}