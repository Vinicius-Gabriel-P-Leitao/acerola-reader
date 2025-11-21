package br.acerola.manga.domain.mapper

import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto

fun MangaMetadataDto.toModel(): MangaMetadata {
    return MangaMetadata(
        name = this.title,
        description = this.description,
        romanji = this.romanji.orEmpty(),
        gender = this.gender.joinToString(separator = ", "),
        publication = this.year ?: 0,
        status = this.status,
        author = this.author.orEmpty()
    )
}

fun MangaMetadata.toDto(): MangaMetadataDto {
    return MangaMetadataDto(
        id = this.id.toString(),
        title = this.name,
        description = this.description,
        romanji = this.romanji,
        gender = this.gender.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        year = this.publication.takeIf { it > 0 },
        status = this.status,
        author = this.author,
    )
}