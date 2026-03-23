package br.acerola.manga.local.translator.ui

import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.chapter.ChapterSourceDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.BannerDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.dto.metadata.manga.source.AnilistSourceDto
import br.acerola.manga.dto.metadata.manga.source.ComicInfoSourceDto
import br.acerola.manga.dto.metadata.manga.source.MangaSourcesDto
import br.acerola.manga.dto.metadata.manga.source.MangadexSourceDto
import br.acerola.manga.local.entity.category.Category
import br.acerola.manga.local.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.entity.metadata.ChapterMetadata
import br.acerola.manga.local.entity.metadata.relationship.Author
import br.acerola.manga.local.entity.metadata.relationship.Banner
import br.acerola.manga.local.entity.metadata.relationship.Cover
import br.acerola.manga.local.entity.metadata.relationship.Genre
import br.acerola.manga.local.entity.metadata.source.AnilistSource
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.entity.metadata.source.MangadexSource
import br.acerola.manga.local.entity.relation.MetadataRelations
import br.acerola.manga.pattern.MetadataSource

fun MetadataRelations.toViewDto(): MangaMetadataDto {
    return MangaMetadataDto(
        id = this.remoteInfo.id,
        title = this.remoteInfo.title,
        description = this.remoteInfo.description,
        romanji = this.remoteInfo.romanji,
        year = this.remoteInfo.publication,
        status = this.remoteInfo.status,
        authors = this.author.firstOrNull()?.toViewDto(),
        cover = this.cover.firstOrNull()?.toViewDto(),
        banner = this.banner.firstOrNull()?.toViewDto(),
        genre = this.genre.map { it.toViewDto() },
        mangaDirectoryFk = this.remoteInfo.mangaDirectoryFk,
        syncSource = MetadataSource.from(this.remoteInfo.syncSource),
        sources = MangaSourcesDto(
            mangadex = this.mangadexSource?.toViewDto(),
            anilist = this.anilistSource?.toViewDto(),
            comicInfo = this.comicInfoSource?.toViewDto()
        )
    )
}

fun MangadexSource.toViewDto(): MangadexSourceDto = MangadexSourceDto(
    mangadexId = mangadexId,
    anilistId = anilistId,
    amazonUrl = amazonUrl,
    ebookjapanUrl = ebookjapanUrl,
    rawUrl = rawUrl,
    engtlUrl = engtlUrl
)

fun AnilistSource.toViewDto(): AnilistSourceDto = AnilistSourceDto(
    anilistId = anilistId,
    averageScore = averageScore,
    popularity = popularity,
    trending = trending,
    coverImage = coverImage,
    bannerImage = bannerImage
)

fun ComicInfoSource.toViewDto(): ComicInfoSourceDto = ComicInfoSourceDto(
    localHash = localHash
)

fun Category.toViewDto(): CategoryDto = CategoryDto(
    id = id, name = name, color = color
)

fun Author.toViewDto(): AuthorDto = AuthorDto(
    id = id.toString(), name = name, type = type.type
)

fun Genre.toViewDto(): GenreDto = GenreDto(
    id = id.toString(), name = genre
)

fun Cover.toViewDto(): CoverDto = CoverDto(
    id = id.toString(), fileName = fileName, url = url,
)

fun Banner.toViewDto(): BannerDto = BannerDto(
    id = id.toString(), fileName = fileName, url = url
)

fun ChapterMetadata.toViewDto(
    sources: List<ChapterDownloadSource>
): ChapterFeedDto {
    return ChapterFeedDto(
        id = id,
        title = title.orEmpty(),
        chapter = chapter,
        pageCount = pageCount,
        scanlation = scanlation.orEmpty(),
        source = sources.sortedBy { it.pageNumber }.map { it.toViewDto() })
}

fun ChapterDownloadSource.toViewDto(): ChapterSourceDto {
    return ChapterSourceDto(
        pageNumber = pageNumber, imageUrl = imageUrl, downloaded = downloaded
    )
}

fun List<ChapterMetadata>.toViewPageDto(
    sources: List<ChapterDownloadSource> = emptyList(), pageSize: Int = this.size, total: Int = this.size, page: Int = 0
): ChapterRemoteInfoPageDto {
    return ChapterRemoteInfoPageDto(
        items = this.map { it.toViewDto(sources.filter { source -> source.chapterFk == it.id }) },
        pageSize = pageSize,
        total = total,
        page = page
    )
}
