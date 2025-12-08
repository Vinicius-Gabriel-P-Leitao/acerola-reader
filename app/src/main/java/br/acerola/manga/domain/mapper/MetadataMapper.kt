package br.acerola.manga.domain.mapper

import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.domain.model.relation.MetadataWithRelations
import br.acerola.manga.shared.dto.metadata.AuthorDto
import br.acerola.manga.shared.dto.metadata.CoverDto
import br.acerola.manga.shared.dto.metadata.GenreDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto

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