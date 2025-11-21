package br.acerola.manga.domain.builder

import android.util.Log
import br.acerola.manga.shared.dto.mangadex.MangaData
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import com.google.gson.GsonBuilder

object MetadataBuilder {
    fun fromMangaData(mangaData: MangaData): MangaMetadataDto {
        val gsonPretty = GsonBuilder().setPrettyPrinting().create()

        Log.d(
            "MetadataBuilder",
            gsonPretty.toJson(mangaData)
        )

        val attributes = mangaData.attributes

        val author: String? = mangaData.authorName
        val genres: List<String> = attributes.tags.mapNotNull { tag -> tag.attributes.name }
        val romanji: String? = attributes.altTitlesList.flatMap { it.entries }.find { it.key == "ja-ro" }?.value
            ?: attributes.titleMap["ja-ro"]

        return MangaMetadataDto(
            id = mangaData.id,
            title = attributes.title ?: "Untitled",
            description = attributes.description ?: "",
            romanji = romanji,
            gender = genres,
            year = attributes.year,
            status = attributes.status,
            author = author
        )
    }

    fun fromMangaDataList(dataList: List<MangaData>): List<MangaMetadataDto> =
        dataList.map { fromMangaData(mangaData = it) }
}