package br.acerola.comic.local.translator.ui

import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.chapter.ChapterSourceDto
import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.BannerDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.CoverDto
import br.acerola.comic.dto.metadata.comic.GenreDto
import br.acerola.comic.dto.metadata.comic.source.AnilistSourceDto
import br.acerola.comic.dto.metadata.comic.source.ComicInfoSourceDto
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto
import br.acerola.comic.dto.metadata.comic.source.MangadexSourceDto
import br.acerola.comic.dto.view.ComicSummaryDto
import br.acerola.comic.local.entity.category.Category
import br.acerola.comic.local.entity.metadata.ChapterDownloadSource
import br.acerola.comic.local.entity.metadata.ChapterMetadata
import br.acerola.comic.local.entity.metadata.relationship.Author
import br.acerola.comic.local.entity.metadata.relationship.Banner
import br.acerola.comic.local.entity.metadata.relationship.Cover
import br.acerola.comic.local.entity.metadata.relationship.Genre
import br.acerola.comic.local.entity.metadata.source.AnilistSource
import br.acerola.comic.local.entity.metadata.source.ComicInfoSource
import br.acerola.comic.local.entity.metadata.source.MangadexSource
import br.acerola.comic.local.entity.relation.MetadataRelations
import br.acerola.comic.local.entity.view.ComicSummaryView
import br.acerola.comic.pattern.metadata.MetadataSource

fun MetadataRelations.toViewDto(): ComicMetadataDto =
    ComicMetadataDto(
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
        comicDirectoryFk = this.remoteInfo.comicDirectoryFk,
        syncSource = MetadataSource.from(this.remoteInfo.syncSource),
        sources =
            ComicSourcesDto(
                mangadex = this.mangadexSource?.toViewDto(),
                anilist = this.anilistSource?.toViewDto(),
                comicInfo = this.comicInfoSource?.toViewDto(),
            ),
    )

fun ComicSummaryView.toViewDto(): ComicSummaryDto =
    ComicSummaryDto(
        directoryId = directoryId,
        folderName = folderName,
        folderCover = folderCover,
        folderBanner = folderBanner,
        externalSync = externalSync,
        metadataTitle = metadataTitle,
        activeSource = MetadataSource.from(activeSource),
        metadataId = metadataId,
    )

fun MangadexSource.toViewDto(): MangadexSourceDto =
    MangadexSourceDto(
        mangadexId = mangadexId,
        anilistId = anilistId,
        amazonUrl = amazonUrl,
        ebookjapanUrl = ebookjapanUrl,
        rawUrl = rawUrl,
        engtlUrl = engtlUrl,
    )

fun AnilistSource.toViewDto(): AnilistSourceDto =
    AnilistSourceDto(
        anilistId = anilistId,
        averageScore = averageScore,
        popularity = popularity,
        trending = trending,
        coverImage = coverImage,
        bannerImage = bannerImage,
    )

fun ComicInfoSource.toViewDto(): ComicInfoSourceDto =
    ComicInfoSourceDto(
        localHash = localHash,
    )

fun Category.toViewDto(): CategoryDto =
    CategoryDto(
        id = id,
        name = name,
        color = color,
    )

fun Author.toViewDto(): AuthorDto =
    AuthorDto(
        id = id.toString(),
        name = name,
        type = type.type,
    )

fun Genre.toViewDto(): GenreDto =
    GenreDto(
        id = id.toString(),
        name = genre,
    )

fun Cover.toViewDto(): CoverDto =
    CoverDto(
        id = id.toString(),
        fileName = fileName,
        url = url,
    )

fun Banner.toViewDto(): BannerDto =
    BannerDto(
        id = id.toString(),
        fileName = fileName,
        url = url,
    )

fun ChapterMetadata.toViewDto(sources: List<ChapterDownloadSource>): ChapterFeedDto =
    ChapterFeedDto(
        id = id,
        title = title.orEmpty(),
        chapter = chapter,
        pageCount = pageCount,
        scanlation = scanlation.orEmpty(),
        source = sources.sortedBy { it.pageNumber }.map { it.toViewDto() },
    )

fun ChapterDownloadSource.toViewDto(): ChapterSourceDto =
    ChapterSourceDto(
        pageNumber = pageNumber,
        imageUrl = imageUrl,
        downloaded = downloaded,
    )

fun List<ChapterMetadata>.toViewPageDto(
    sources: List<ChapterDownloadSource> = emptyList(),
    pageSize: Int = this.size,
    total: Int = this.size,
    page: Int = 0,
): ChapterRemoteInfoPageDto =
    ChapterRemoteInfoPageDto(
        items = this.map { it.toViewDto(sources.filter { source -> source.chapterFk == it.id }) },
        pageSize = pageSize,
        total = total,
        page = page,
    )
