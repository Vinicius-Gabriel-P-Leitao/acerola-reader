package br.acerola.manga.domain.builder

import br.acerola.manga.shared.dto.mangadex.MangaData
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto

object MetadataBuilder {
    fun fromMangaData(mangaData: MangaData): MangaMetadataDto {
        val attributes = mangaData.attributes
        val genres: List<String> = attributes.tags.mapNotNull { tag -> tag.attributes.name }
        val author: String? = mangaData.relationships.firstOrNull { it.type == "author" }?.related

        return MangaMetadataDto(
            id = mangaData.id,
            title = attributes.title ?: "Untitled",
            description = attributes.description ?: "",
            romanji = attributes.titleMap["ja-ro"],
            gender = genres,
            year = attributes.year,
            author = author
        )
    }

    fun fromMangaDataList(dataList: List<MangaData>): List<MangaMetadataDto> =
        dataList.map { fromMangaData(mangaData = it) }
}