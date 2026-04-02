package br.acerola.manga.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.acerola.manga.local.converter.AcerolaTypeConverters
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
import br.acerola.manga.local.entity.archive.ChapterArchive
import br.acerola.manga.local.entity.archive.ChapterTemplate
import br.acerola.manga.local.entity.archive.MangaDirectory
import br.acerola.manga.local.entity.category.Category
import br.acerola.manga.local.entity.category.MangaCategory
import br.acerola.manga.local.entity.history.ChapterRead
import br.acerola.manga.local.entity.history.ReadingHistory
import br.acerola.manga.local.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.entity.metadata.ChapterMetadata
import br.acerola.manga.local.entity.metadata.MangaMetadata
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
        ChapterTemplate::class,
        MangaMetadata::class,
        ChapterArchive::class,
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
        MangaCategory::class
    ],
    views = [
        br.acerola.manga.local.entity.view.MangaSummaryView::class
    ],
    exportSchema = false,
    version = 4,
)
@TypeConverters(AcerolaTypeConverters::class)
abstract class AcerolaDatabase : RoomDatabase() {
    abstract fun chapterArchiveDao(): ChapterArchiveDao
    abstract fun mangaDirectoryDao(): MangaDirectoryDao
    abstract fun chapterTemplateDao(): ChapterTemplateDao
    abstract fun mangaRemoteInfoDao(): MangaMetadataDao
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
    abstract fun mangaSummaryDao(): br.acerola.manga.local.dao.view.MangaSummaryDao
}
