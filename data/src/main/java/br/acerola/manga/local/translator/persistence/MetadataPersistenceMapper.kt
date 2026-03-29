package br.acerola.manga.local.translator.persistence

import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.BannerDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.local.entity.category.Category
import br.acerola.manga.local.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.entity.metadata.ChapterMetadata
import br.acerola.manga.local.entity.metadata.MangaMetadata
import br.acerola.manga.local.entity.metadata.relationship.Author
import br.acerola.manga.local.entity.metadata.relationship.Banner
import br.acerola.manga.local.entity.metadata.relationship.Cover
import br.acerola.manga.local.entity.metadata.relationship.Genre
import br.acerola.manga.local.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.local.entity.metadata.source.AnilistSource
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.entity.metadata.source.MangadexSource

fun CategoryDto.toEntity(): Category = Category(
    id = id, name = name, color = color
)

fun AuthorDto.toEntity(mangaId: Long): Author = Author(
    name = name, type = TypeAuthor.getByType(type), mangaRemoteInfoFk = mangaId
)

fun GenreDto.toEntity(mangaId: Long): Genre = Genre(
    genre = name, mangaRemoteInfoFk = mangaId
)

fun CoverDto.toEntity(mangaId: Long): Cover = Cover(
    fileName = fileName, url = url, mangaRemoteInfoFk = mangaId
)

fun BannerDto.toEntity(mangaId: Long): Banner = Banner(
    fileName = fileName, url = url, mangaRemoteInfoFk = mangaId
)

fun MangaMetadataDto.toEntity(
    mangaDirectoryFk: Long? = this.mangaDirectoryFk,
    syncSource: String? = this.syncSource?.source
): MangaMetadata = MangaMetadata(
    id = this.id ?: 0L,
    title = this.title,
    description = this.description,
    romanji = this.romanji.orEmpty(),
    status = this.status,
    publication = this.year ?: 0,
    mangaDirectoryFk = mangaDirectoryFk,
    syncSource = syncSource
)

fun ChapterMetadataDto.toEntity(mangaRemoteInfoFk: Long): ChapterMetadata = ChapterMetadata(
    chapter = chapter!!,
    title = title,
    pageCount = pages,
    scanlation = scanlator,
    mangaRemoteInfoFk = mangaRemoteInfoFk
)

fun ChapterMetadataDto.toDownloadSourcesEntities(chapterFk: Long): List<ChapterDownloadSource> =
    pageUrls.mapIndexed { index, url ->
        ChapterDownloadSource(
            pageNumber = index, imageUrl = url, downloaded = false, chapterFk = chapterFk
        )
    }

fun MangaMetadataDto.toMangadexSourceEntity(mangaRemoteInfoFk: Long): MangadexSource {
    val mangadex = sources?.mangadex ?: throw IllegalStateException("MangaDex source is null in DTO")
    return MangadexSource(
        mangadexId = mangadex.mangadexId,
        anilistId = mangadex.anilistId,
        amazonUrl = mangadex.amazonUrl,
        ebookjapanUrl = mangadex.ebookjapanUrl,
        rawUrl = mangadex.rawUrl,
        engtlUrl = mangadex.engtlUrl,
        mangaRemoteInfoFk = mangaRemoteInfoFk
    )
}

fun MangaMetadataDto.toAnilistSourceEntity(mangaRemoteInfoFk: Long): AnilistSource {
    val anilist = sources?.anilist ?: throw IllegalStateException("AniList source is null in DTO")
    return AnilistSource(
        anilistId = anilist.anilistId,
        averageScore = anilist.averageScore,
        popularity = anilist.popularity,
        trending = anilist.trending,
        coverImage = anilist.coverImage,
        bannerImage = anilist.bannerImage,
        mangaRemoteInfoFk = mangaRemoteInfoFk
    )
}

fun MangaMetadataDto.toComicInfoSourceEntity(mangaRemoteInfoFk: Long): ComicInfoSource {
    val comicInfo = sources?.comicInfo
    return ComicInfoSource(
        localHash = comicInfo?.localHash ?: "local-${this.title.hashCode()}",
        mangaRemoteInfoFk = mangaRemoteInfoFk
    )
}
