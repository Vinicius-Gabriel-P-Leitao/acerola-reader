package br.acerola.manga.local.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.acerola.manga.local.converter.DatabaseConverters
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.cover.CoverDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre

@Database(
    entities = [
        MangaDirectory::class,
        MangaRemoteInfo::class,
        ChapterArchive::class,
        ChapterRemoteInfo::class,
        ChapterDownloadSource::class,
        Author::class,
        Genre::class,
        Cover::class
    ],
    exportSchema = false,
    version = 1,
)
@TypeConverters(DatabaseConverters::class)
abstract class DatabaseAcerola : RoomDatabase() {
    abstract fun chapterArchiveDao(): ChapterArchiveDao
    abstract fun mangaDirectoryDao(): MangaDirectoryDao
    abstract fun mangaMangaRemoteInfoDao(): MangaRemoteInfoDao
    abstract fun chapterRemoteInfoDao(): ChapterRemoteInfoDao
    abstract fun chapterDownloadSourceDao(): ChapterDownloadSourceDao
    abstract fun authorDao(): AuthorDao
    abstract fun coverDao(): CoverDao
    abstract fun genreDao(): GenreDao
}