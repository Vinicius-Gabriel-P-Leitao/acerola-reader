package br.acerola.manga.local.mapper

import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.chapter.ChapterSourceDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.local.database.entity.relation.RemoteInfoRelations

fun RemoteInfoRelations.toDto(): MangaRemoteInfoDto {
    return MangaRemoteInfoDto(
        id = this.remoteInfo.id,
        mirrorId = this.remoteInfo.mirrorId,
        title = this.remoteInfo.title,
        description = this.remoteInfo.description,
        romanji = this.remoteInfo.romanji,
        year = this.remoteInfo.publication,
        status = this.remoteInfo.status,
        authors = this.author.firstOrNull()?.toDto(),
        cover = this.cover.firstOrNull()?.toDto(),
        genre = this.genre.map { it.toDto() },
    )
}

fun Author.toDto(): AuthorDto {
    return AuthorDto(
        id = mirrorId, name = name, type = type.type
    )
}

fun AuthorDto.toModel(mangaId: Long): Author {
    return Author(
        name = name, type = TypeAuthor.getByType(type), mirrorId = id, mangaRemoteInfoFk = mangaId
    )
}

fun Genre.toDto(): GenreDto {
    return GenreDto(
        id = mirrorId, name = genre
    )
}

fun GenreDto.toModel(mangaId: Long): Genre {
    return Genre(
        genre = name, mirrorId = id, mangaRemoteInfoFk = mangaId
    )
}

fun Cover.toDto(): CoverDto {
    return CoverDto(
        id = mirrorId, fileName = fileName, url = url,
    )
}

fun CoverDto.toModel(mangaId: Long): Cover {
    return Cover(
        fileName = fileName, url = url, mirrorId = id, mangaRemoteInfoFk = mangaId
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
        mirrorId = this.mirrorId,
        title = this.title,
        description = this.description,
        romanji = this.romanji.orEmpty(),
        status = this.status,
        publication = this.year ?: 0
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
