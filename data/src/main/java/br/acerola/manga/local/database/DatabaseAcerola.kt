package br.acerola.manga.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.acerola.manga.local.converter.DatabaseConverters
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.history.ReadingHistoryDao
import br.acerola.manga.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.BannerDao
import br.acerola.manga.local.dao.metadata.relationship.CoverDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.AnilistSourceDao
import br.acerola.manga.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.manga.local.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.local.entity.archive.ChapterArchive
import br.acerola.manga.local.entity.archive.MangaDirectory
import br.acerola.manga.local.entity.history.ChapterRead
import br.acerola.manga.local.entity.history.ReadingHistory
import br.acerola.manga.local.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.entity.metadata.relationship.Author
import br.acerola.manga.local.entity.metadata.relationship.Banner
import br.acerola.manga.local.entity.metadata.relationship.Cover
import br.acerola.manga.local.entity.metadata.relationship.Genre
import br.acerola.manga.local.entity.metadata.source.AnilistSource
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.entity.metadata.source.MangadexSource

@Database(
    entities = [
        MangaDirectory::class,
        MangaRemoteInfo::class,
        ChapterArchive::class,
        ChapterRemoteInfo::class,
        ChapterDownloadSource::class,
        Author::class,
        Genre::class,
        Cover::class,
        Banner::class,
        ReadingHistory::class,
        ChapterRead::class,
        MangadexSource::class,
        AnilistSource::class,
        ComicInfoSource::class
    ],
    exportSchema = false,
    version = 8,
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
    abstract fun bannerDao(): BannerDao
    abstract fun genreDao(): GenreDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun mangadexSourceDao(): MangadexSourceDao
    abstract fun anilistSourceDao(): AnilistSourceDao
    abstract fun comicInfoSourceDao(): ComicInfoSourceDao
}
