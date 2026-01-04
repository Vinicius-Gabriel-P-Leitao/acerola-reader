package br.acerola.manga.local.mapper

import br.acerola.manga.dto.metadata.chapter.ChapterDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterSourceDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
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

        authors = this.author?.let { author ->
            AuthorDto(
                id = author.mirrorId,
                name = author.name,
                type = author.type.type
            )

        },

        cover = this.cover?.let { cover ->
            CoverDto(
                id = cover.mirrorId,
                fileName = cover.fileName,
                url = cover.url
            )
        },

        genre = this.genre?.let { genre ->
            listOf(
                GenreDto(
                    id = genre.mirrorId,
                    name = genre.genre
                )
            )
        } ?: emptyList()
    )
}

fun ChapterRemoteInfo.toDto(
    sources: List<ChapterDownloadSource>
): ChapterDto {
    return ChapterDto(
        id = id,
        title = title.orEmpty(),
        chapter = chapter,
        pageCount = pageCount,
        scanlation = scanlation.orEmpty(),
        source = sources.sortedBy { it.pageNumber }.map { it.toDto() }
    )
}

fun ChapterDownloadSource.toDto(): ChapterSourceDto {
    return ChapterSourceDto(
        pageNumber = pageNumber,
        imageUrl = imageUrl,
        downloaded = downloaded
    )
}

// FIXME: Os métodos que deveriram ser toModel e toDto do author, cover e genre estão injetados no método
fun MangaRemoteInfoDto.toModel(
    authorId: Long?,
    coverId: Long?,
    genreId: Long?
): MangaRemoteInfo {
    return MangaRemoteInfo(
        mirrorId = this.mirrorId,
        title = this.title,
        description = this.description,
        romanji = this.romanji.orEmpty(),
        status = this.status,
        publication = this.year ?: 0,
        mangaAuthorFk = authorId,
        mangaGenreFk = genreId,
        mangaCoverFk = coverId
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
            pageNumber = index,
            imageUrl = url,
            downloaded = false,
            chapterFk = chapterFk
        )
    }
}
