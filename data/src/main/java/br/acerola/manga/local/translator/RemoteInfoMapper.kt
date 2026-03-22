package br.acerola.manga.local.translator

import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.chapter.ChapterSourceDto
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.BannerDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.source.MangaSourcesDto
import br.acerola.manga.dto.metadata.manga.source.MangadexSourceDto
import br.acerola.manga.dto.metadata.manga.source.AnilistSourceDto
import br.acerola.manga.dto.metadata.manga.source.ComicInfoSourceDto
import br.acerola.manga.local.entity.category.Category
import br.acerola.manga.local.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.entity.metadata.relationship.Author
import br.acerola.manga.local.entity.metadata.relationship.Banner
import br.acerola.manga.local.entity.metadata.relationship.Cover
import br.acerola.manga.local.entity.metadata.relationship.Genre
import br.acerola.manga.local.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.local.entity.metadata.source.AnilistSource
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.entity.metadata.source.MangadexSource
import br.acerola.manga.local.entity.relation.RemoteInfoRelations
import br.acerola.manga.pattern.MetadataSource
import kotlin.collections.map

fun RemoteInfoRelations.toDto(): MangaRemoteInfoDto {
    return MangaRemoteInfoDto(
        id = this.remoteInfo.id,
        title = this.remoteInfo.title,
        description = this.remoteInfo.description,
        romanji = this.remoteInfo.romanji,
        year = this.remoteInfo.publication,
        status = this.remoteInfo.status,
        authors = this.author.firstOrNull()?.toDto(),
        cover = this.cover.firstOrNull()?.toDto(),
        banner = this.banner.firstOrNull()?.toDto(),
        genre = this.genre.map { it.toDto() },
        mangaDirectoryFk = this.remoteInfo.mangaDirectoryFk,
        syncSource = MetadataSource.from(this.remoteInfo.syncSource),
        sources = MangaSourcesDto(
            mangadex = this.mangadexSource?.toDto(),
            anilist = this.anilistSource?.toDto(),
            comicInfo = this.comicInfoSource?.toDto()
        )
    )
}

fun MangadexSource.toDto(): MangadexSourceDto = MangadexSourceDto(
    mangadexId = mangadexId,
    anilistId = anilistId,
    amazonUrl = amazonUrl,
    ebookjapanUrl = ebookjapanUrl,
    rawUrl = rawUrl,
    engtlUrl = engtlUrl
)

fun AnilistSource.toDto(): AnilistSourceDto = AnilistSourceDto(
    anilistId = anilistId,
    averageScore = averageScore,
    popularity = popularity,
    trending = trending,
    coverImage = coverImage,
    bannerImage = bannerImage
)

fun ComicInfoSource.toDto(): ComicInfoSourceDto = ComicInfoSourceDto(
    localHash = localHash
)

fun Category.toCategoryDto(): CategoryDto {
    return CategoryDto(
        id = id, name = name, color = color
    )
}

fun CategoryDto.toCategoryModel(): Category {
    return Category(
        id = id, name = name, color = color
    )
}

fun Author.toDto(): AuthorDto {
    return AuthorDto(
        id = id.toString(), name = name, type = type.type
    )
}

fun AuthorDto.toModel(mangaId: Long): Author {
    return Author(
        name = name, type = TypeAuthor.getByType(type), mangaRemoteInfoFk = mangaId
    )
}

fun Genre.toDto(): GenreDto {
    return GenreDto(
        id = id.toString(), name = genre
    )
}

fun GenreDto.toModel(mangaId: Long): Genre {
    return Genre(
        genre = name, mangaRemoteInfoFk = mangaId
    )
}

fun Cover.toDto(): CoverDto {
    return CoverDto(
        id = id.toString(), fileName = fileName, url = url,
    )
}

fun CoverDto.toModel(mangaId: Long): Cover {
    return Cover(
        fileName = fileName, url = url, mangaRemoteInfoFk = mangaId
    )
}

fun Banner.toDto(): BannerDto {
    return BannerDto(
        id = id.toString(), fileName = fileName, url = url
    )
}

fun BannerDto.toModel(mangaId: Long): Banner {
    return Banner(
        fileName = fileName, url = url, mangaRemoteInfoFk = mangaId
    )
}

fun ChapterRemoteInfo.toDto(
    sources: List<ChapterDownloadSource>
): ChapterFeedDto {
    return ChapterFeedDto(
        id = id,
        title = title.orEmpty(),
        chapter = chapter,
        pageCount = pageCount,
        scanlation = scanlation.orEmpty(),
        source = sources.sortedBy { it.pageNumber }.map { it.toDto() })
}

fun ChapterDownloadSource.toDto(): ChapterSourceDto {
    return ChapterSourceDto(
        pageNumber = pageNumber, imageUrl = imageUrl, downloaded = downloaded
    )
}

fun MangaRemoteInfoDto.toModel(): MangaRemoteInfo {
    return MangaRemoteInfo(
        id = this.id ?: 0L,
        title = this.title,
        description = this.description,
        romanji = this.romanji.orEmpty(),
        status = this.status,
        publication = this.year ?: 0,
        mangaDirectoryFk = this.mangaDirectoryFk,
        syncSource = this.syncSource?.source
    )
}

fun ChapterRemoteInfoDto.toModel(
    mangaRemoteInfoFk: Long
): ChapterRemoteInfo {
    return ChapterRemoteInfo(
        chapter = chapter!!,
        title = title,
        pageCount = pages,
        scanlation = scanlator,
        mangaRemoteInfoFk = mangaRemoteInfoFk
    )
}

fun ChapterRemoteInfoDto.toDownloadSources(
    chapterFk: Long
): List<ChapterDownloadSource> {
    return pageUrls.mapIndexed { index, url ->
        ChapterDownloadSource(
            pageNumber = index, imageUrl = url, downloaded = false, chapterFk = chapterFk
        )
    }
}

fun MangaRemoteInfoDto.toMangadexSource(mangaRemoteInfoFk: Long): MangadexSource {
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

fun MangaRemoteInfoDto.toAnilistSource(mangaRemoteInfoFk: Long): AnilistSource {
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

fun List<ChapterRemoteInfo>.toPageDto(
    sources: List<ChapterDownloadSource> = emptyList(), pageSize: Int = this.size, total: Int = this.size, page: Int = 0
): ChapterRemoteInfoPageDto {
    return ChapterRemoteInfoPageDto(
        items = this.map { it.toDto(sources.filter { source -> source.chapterFk == it.id }) },
        pageSize = pageSize,
        total = total,
        page = page
    )
}
