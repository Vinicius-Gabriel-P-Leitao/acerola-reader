package br.acerola.comic.config.preference.types

enum class ChapterSortType(
    val key: String,
) {
    NUMBER(key = "NUMBER"),
    LAST_UPDATE(key = "LAST_UPDATE"),
    ;

    companion object {
        fun fromKey(key: String?): ChapterSortType = entries.firstOrNull { it.key == key } ?: NUMBER
    }
}

data class ChapterSortPreferenceData(
    val type: ChapterSortType,
    val direction: SortDirection,
)
