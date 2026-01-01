package br.acerola.manga.local.mapper

import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.local.database.entity.metadata.MangaMetadata
import br.acerola.manga.local.database.entity.relation.MetadataWithRelations

fun MetadataWithRelations.toDto(): MangaMetadataDto {
    return MangaMetadataDto(
        id = this.metadata.mirrorId,
        title = this.metadata.name,
        description = this.metadata.description,
        romanji = this.metadata.romanji,
        year = this.metadata.publication,
        status = this.metadata.status,

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

        gender = this.gender?.let { gen ->
            listOf(
                GenreDto(
                    id = gen.mirrorId,
                    name = gen.gender
                )
            )
        } ?: emptyList()
    )
}

fun MangaMetadataDto.toModel(
    authorId: Long?,
    coverId: Long?,
    genderId: Long?
): MangaMetadata {
    return MangaMetadata(
        mirrorId = this.id,
        name = this.title,
        description = this.description,
        romanji = this.romanji.orEmpty(),
        status = this.status,
        publication = this.year ?: 0,
        mangaAuthorFk = authorId,
        mangaGenderFk = genderId,
        mangaCoverFk = coverId
    )
}