package br.acerola.manga.shared.dto.mangadex

import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.model.metadata.author.TypeAuthor
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
) {

    private val bestAuthorMatch: Relationship?
        get() = relationships.find { it.type == TypeAuthor.AUTHOR.type }
            ?: relationships.find { it.type == TypeAuthor.ARTIST.type }

    val authorName: String?
        get() = bestAuthorMatch?.attributes?.name

    val authorId: String?
        get() = bestAuthorMatch?.id

    val authorType: String?
        get() = bestAuthorMatch?.type

    val coverFileName: String?
        get() = relationships.find { it.type == "cover_art" }?.attributes?.fileName

    val coverId: String?
        get() = relationships.find { it.type == "cover_art" }?.id

    fun getCoverUrl(): String? {
        return if (coverFileName != null) {
            "${BuildConfig.MANGADEX_UPLOAD_URL}/covers/$id/$coverFileName"
        } else null
    }
}

data class MangaAttributes(
    @SerializedName(value = "title") val titleMap: Map<String, String>,
    @SerializedName(value = "altTitles") val altTitlesList: List<Map<String, String>> = emptyList(),
    @SerializedName(value = "description") val descriptionMap: Map<String, String> = emptyMap(),
    val isLocked: Boolean = false,
    val links: Links?,
    val status: String,
    val year: Int? = null,
    val tags: List<Tag> = emptyList(),
    val latestUploadedChapter: String? = null
) {
    val title: String? get() = titleMap["pt-br"] ?: titleMap["en"] ?: titleMap["ja-ro"]
    val description: String?
        get() = descriptionMap["pt-br"] ?: descriptionMap["en"] ?: descriptionMap["ja"]
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
    val related: String? = null,
    val attributes: RelationshipAttributes? = null
)

data class RelationshipAttributes(
    val name: String? = null,
    val volume: String? = null,
    val fileName: String? = null,
)