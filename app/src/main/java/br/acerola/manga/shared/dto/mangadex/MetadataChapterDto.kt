package br.acerola.manga.shared.dto.mangadex

data class MetadataChapterDto(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<ChapterRelationship> = emptyList()
) {
    val volume: String?
        get() = attributes.volume

    val chapter: String?
        get() = attributes.chapter

    val title: String?
        get() = attributes.title

    val pages: Int
        get() = attributes.pages

    val scanlationGroups: List<ChapterRelationship>
        get() = relationships.filter { it.type == "scanlation_group" }
}

data class ChapterAttributes(
    val volume: String?,
    val chapter: String?,
    val title: String?,
    val pages: Int = 0
)

data class ChapterRelationship(
    val id: String,
    val type: String,
    val attributes: ChapterRelationshipAttributes? = null
)

data class ChapterRelationshipAttributes(
    val name: String? = null,
    val focusedLanguages: List<String>? = emptyList()
)