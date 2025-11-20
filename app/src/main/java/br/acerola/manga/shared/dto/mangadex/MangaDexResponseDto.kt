package br.acerola.manga.shared.dto.mangadex

import com.google.gson.annotations.SerializedName

data class MangaDexResponse(
    val result: String,
    val response: String,
    val data: List<MangaData>,
    val limit: Int,
    val offset: Int,
    val total: Int
)

data class MangaData(
    val id: String,
    val type: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList()
)

data class MangaAttributes(
    @SerializedName(value = "title") val titleMap: Map<String, String>,
    @SerializedName(value = "altTitles") val altTitlesList: List<Map<String, String>> = emptyList(),
    @SerializedName(value = "description") val descriptionMap: Map<String, String> = emptyMap(),
    val isLocked: Boolean = false,
    val links: Links,
    val status: String,
    val tags: List<Tag> = emptyList(),
    val year: Int? = null,
    val latestUploadedChapter: String? = null
) {
    val title: String? get() = titleMap["pt-br"] ?: titleMap["en"] ?: titleMap["ja-ro"]
    val description: String? get() = descriptionMap["pt-br"] ?: descriptionMap["en"] ?: descriptionMap["ja"]
}

data class Links(
    val al: String? = null,
    val ap: String? = null,
    val kt: String? = null,
    val mu: String? = null,
    val mal: String? = null,
    val raw: String? = null
)

data class Tag(
    val id: String,
    val type: String,
    val attributes: TagAttributes
)

data class TagAttributes(
    @SerializedName(value = "name") val nameMap: Map<String, String>,
    val group: String,
    val version: Int
) {
    val name: String? get() = nameMap["pt-br"] ?: nameMap["en"] ?: nameMap["ja-ro"]
}

data class Relationship(
    val id: String,
    val type: String,
    val related: String? = null
)
