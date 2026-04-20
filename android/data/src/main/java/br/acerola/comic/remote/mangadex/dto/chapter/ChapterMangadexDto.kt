package br.acerola.comic.remote.mangadex.dto.chapter

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChapterMangadexDto(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<ChapterRelationship> = emptyList(),
) {
    val volume: String?
        get() = attributes.volume

    val chapter: String?
        get() = attributes.chapter

    val title: String?
        get() = attributes.title

    val pages: Int
        get() = attributes.pages

    val version: Int
        get() = attributes.version

    val scanlationGroups: List<ChapterRelationship>
        get() = relationships.filter { it.type == "scanlation_group" }
}

@JsonClass(generateAdapter = true)
data class ChapterAttributes(
    val volume: String?,
    val chapter: String?,
    val title: String?,
    val translatedLanguage: String? = null,
    val pages: Int = 0,
    val version: Int,
)

@JsonClass(generateAdapter = true)
data class ChapterRelationship(
    val id: String,
    val type: String,
    val attributes: ChapterRelationshipAttributes? = null,
)

@JsonClass(generateAdapter = true)
data class ChapterRelationshipAttributes(
    val name: String? = null,
    val focusedLanguages: List<String>? = emptyList(),
)
