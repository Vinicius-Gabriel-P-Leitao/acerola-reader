package br.acerola.comic.config.preference.types

enum class ComicSortType(
    val key: String,
) {
    TITLE(key = "TITLE"),
    CHAPTER_COUNT(key = "CHAPTER_COUNT"),
    LAST_UPDATE(key = "LAST_UPDATE"),
    ;

    companion object {
        fun fromKey(key: String?): ComicSortType = entries.firstOrNull { it.key == key } ?: TITLE
    }
}

enum class SortDirection(
    val key: String,
) {
    ASCENDING(key = "ASCENDING"),
    DESCENDING(key = "DESCENDING"),
    ;

    companion object {
        fun fromKey(key: String?): SortDirection = entries.firstOrNull { it.key == key } ?: ASCENDING
    }
}

data class HomeSortPreference(
    val type: ComicSortType,
    val direction: SortDirection,
)
