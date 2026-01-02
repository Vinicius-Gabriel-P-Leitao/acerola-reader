package br.acerola.manga.local.mapper

import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.relation.RemoteInfoRelations

fun RemoteInfoRelations.toDto(): MangaRemoteInfoDto {
    return MangaRemoteInfoDto(
        id = this.remoteInfo.mirrorId,
        title = this.remoteInfo.name,
        description = this.remoteInfo.description,
        romanji = this.remoteInfo.romanji,
        year = this.remoteInfo.publication,
        status = this.remoteInfo.status,

        authors = this.author?.let { auth ->
            AuthorDto(
                id = auth.mirrorId,
                name = auth.name,
                type = auth.type.type
            )

        },

        cover = this.cover?.let { cov ->
            CoverDto(
                id = cov.mirrorId,
                fileName = cov.fileName,
                url = cov.url
            )
        },

        genre = this.genre?.let { gen ->
            listOf(
                GenreDto(
                    id = gen.mirrorId,
                    name = gen.genre
                )
            )
        } ?: emptyList()
    )
}

fun MangaRemoteInfoDto.toModel(
    authorId: Long?,
    coverId: Long?,
    genreId: Long?
): MangaRemoteInfo {
    return MangaRemoteInfo(
        mirrorId = this.id,
        name = this.title,
        description = this.description,
        romanji = this.romanji.orEmpty(),
        status = this.status,
        publication = this.year ?: 0,
        mangaAuthorFk = authorId,
        mangaGenreFk = genreId,
        mangaCoverFk = coverId
    )
}