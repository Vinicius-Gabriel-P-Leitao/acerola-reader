package br.acerola.manga.domain.builder

import br.acerola.manga.shared.dto.mangadex.MetadataMangaDto
import br.acerola.manga.shared.dto.metadata.AuthorDto
import br.acerola.manga.shared.dto.metadata.CoverDto
import br.acerola.manga.shared.dto.metadata.GenreDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto

object MangaMetadataBuilder {
    fun fromMangaData(metadataMangaDto: MetadataMangaDto): MangaMetadataDto {
        val attributes = metadataMangaDto.attributes

        val authors = if (metadataMangaDto.authorName != null && metadataMangaDto.authorId !=null) {
            AuthorDto(
                id = metadataMangaDto.authorId!!,
                name = metadataMangaDto.authorName!!,
                type = metadataMangaDto.authorType!!
            )
        } else null

        val coverDto = if (metadataMangaDto.coverFileName != null && metadataMangaDto.coverId != null) {
            CoverDto(
                id = metadataMangaDto.coverId!!,
                fileName = metadataMangaDto.coverFileName!!,
                url = metadataMangaDto.getCoverUrl() ?: ""
            )
        } else null

        val genresList: List<GenreDto> = attributes.tags.mapNotNull { tag ->
            val name = tag.attributes.name
            if (!name.isNullOrBlank()) {
                GenreDto(id = tag.id, name = name)
            } else null
        }

        val romanji: String? = attributes.altTitlesList
            .flatMap { it.entries }
            .find { it.key == "ja-ro" }?.value
            ?: attributes.titleMap["ja-ro"]

        // TODO: String para valores default
        return MangaMetadataDto(
            id = metadataMangaDto.id,
            title = attributes.title ?: "Sem Título",
            description = attributes.description ?: "",
            romanji = romanji,
            year = attributes.year,
            status = attributes.status,
            cover = coverDto,
            gender = genresList,
            authors = authors
        )
    }

    fun fromMangaDataList(dataList: List<MetadataMangaDto>): List<MangaMetadataDto> =
        dataList.map { fromMangaData(metadataMangaDto = it) }
}