package br.acerola.comic.local.translator.persistence

import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.GenreDto
import br.acerola.comic.local.entity.metadata.ComicMetadata
import br.acerola.comic.local.entity.metadata.relationship.Author
import br.acerola.comic.local.entity.metadata.relationship.Genre
import br.acerola.comic.local.entity.metadata.relationship.TypeAuthor
import br.acerola.comic.local.entity.metadata.source.AnilistSource
import br.acerola.comic.local.entity.metadata.source.MangadexSource

fun ComicMetadataDto.toEntity(
    comicDirectoryFk: Long? = this.comicDirectoryFk,
    syncSource: String? = this.syncSource?.source,
): ComicMetadata =
    ComicMetadata(
        id = this.id ?: 0L,
        title = this.title,
        description = this.description,
        status = this.status,
        publication = this.year ?: 0,
        comicDirectoryFk = comicDirectoryFk,
        syncSource = syncSource,
    )

fun AuthorDto.toEntity(comicRemoteInfoFk: Long): Author =
    Author(
        name = name,
        type = TypeAuthor.getByType(type),
        comicRemoteInfoFk = comicRemoteInfoFk,
    )

fun GenreDto.toEntity(comicRemoteInfoFk: Long): Genre =
    Genre(
        genre = name,
        comicRemoteInfoFk = comicRemoteInfoFk,
    )

fun ComicMetadataDto.toMangadexSourceEntity(comicRemoteInfoFk: Long): MangadexSource {
    val mangadex = sources?.mangadex ?: throw IllegalStateException("MangaDex source is null in DTO")
    return MangadexSource(
        mangadexId = mangadex.mangadexId,
        comicRemoteInfoFk = comicRemoteInfoFk,
    )
}

fun ComicMetadataDto.toAnilistSourceEntity(comicRemoteInfoFk: Long): AnilistSource {
    val anilist = sources?.anilist ?: throw IllegalStateException("AniList source is null in DTO")
    return AnilistSource(
        anilistId = anilist.anilistId,
        averageScore = anilist.averageScore,
        popularity = anilist.popularity,
        trending = anilist.trending,
        coverImage = anilist.coverImage,
        bannerImage = anilist.bannerImage,
        comicRemoteInfoFk = comicRemoteInfoFk,
    )
}
