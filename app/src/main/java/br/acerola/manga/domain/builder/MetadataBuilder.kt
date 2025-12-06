package br.acerola.manga.domain.builder

import br.acerola.manga.shared.dto.mangadex.MangaData
import br.acerola.manga.shared.dto.metadata.AuthorDto
import br.acerola.manga.shared.dto.metadata.CoverDto
import br.acerola.manga.shared.dto.metadata.GenreDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto

object MetadataBuilder {
    fun fromMangaData(mangaData: MangaData): MangaMetadataDto {
        val attributes = mangaData.attributes

        val authors = if (mangaData.authorName != null && mangaData.authorId !=null) {
            AuthorDto(
                id = mangaData.authorId!!,
                name = mangaData.authorName!!,
                type = mangaData.authorType!!
            )
        } else null

        val coverDto = if (mangaData.coverFileName != null && mangaData.coverId != null) {
            CoverDto(
                id = mangaData.coverId!!,
                fileName = mangaData.coverFileName!!,
                url = mangaData.getCoverUrl() ?: ""
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
            id = mangaData.id,
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

    fun fromMangaDataList(dataList: List<MangaData>): List<MangaMetadataDto> =
        dataList.map { fromMangaData(mangaData = it) }
}