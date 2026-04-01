package br.acerola.manga.remote.mangadex.dto.manga

import br.acerola.manga.data.BuildConfig
import br.acerola.manga.local.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.pattern.LanguagePattern
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaMangadexDto(
    val id: String,
    val type: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList()
) {
    val bestAuthorMatch: Relationship?
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

@JsonClass(generateAdapter = true)
data class MangaAttributes(
    @param:Json(name = "title") val titleMap: Map<String, String>,
    @param:Json(name = "altTitles") val altTitlesList: List<Map<String, String>> = emptyList(),
    @param:Json(name = "description") val descriptionMap: Map<String, String> = emptyMap(),
    val isLocked: Boolean = false,
    val links: Links?,
    val status: String,
    val year: Int? = null,
    val tags: List<Tag> = emptyList(),
    val latestUploadedChapter: String? = null
) {
    fun getTitle(preferredLanguage: String): String? {
        val pattern = LanguagePattern.from(preferredLanguage)?.code
        return titleMap[pattern] ?: titleMap["en"] ?: titleMap["ja-ro"]
    }

    fun getDescription(preferredLanguage: String): String? {
        val pattern = LanguagePattern.from(preferredLanguage)?.code
        return descriptionMap[pattern] ?: descriptionMap["en"] ?: descriptionMap["ja"]
    }
}

@JsonClass(generateAdapter = true)
data class Links(
    val al: String? = null,
    val ap: String? = null,
    val kt: String? = null,
    val mu: String? = null,
    val mal: String? = null,
    val raw: String? = null,
    val amz: String? = null,
    val ebj: String? = null,
    val engtl: String? = null
)

@JsonClass(generateAdapter = true)
data class Tag(
    val id: String,
    val type: String,
    val attributes: TagAttributes
)
 
@JsonClass(generateAdapter = true)
data class TagAttributes(
    @param:Json(name = "name") val nameMap: Map<String, String>,
    val group: String,
    val version: Int
) {
    fun getName(preferredLanguage: String): String? {
        val pattern = LanguagePattern.from(preferredLanguage)?.code
        return nameMap[pattern] ?: nameMap["en"] ?: nameMap["ja-ro"]
    }
}


@JsonClass(generateAdapter = true)
data class Relationship(
    val id: String,
    val type: String,
    val related: String? = null,
    val attributes: RelationshipAttributes? = null
)

@JsonClass(generateAdapter = true)
data class RelationshipAttributes(
    val name: String? = null,
    val volume: String? = null,
    val fileName: String? = null,
)