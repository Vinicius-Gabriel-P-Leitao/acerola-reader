package br.acerola.comic.config.preference.types

enum class ChapterPageSizeType(
    val key: String,
) {
    SHORT(key = "25"),
    MEDIUM(key = "50"),
    LARGE(key = "100"),
    ;

    companion object {
        fun fromKey(key: String?): ChapterPageSizeType = entries.firstOrNull { it.key == key } ?: SHORT
    }
}
