package br.acerola.comic.config.preference.types

enum class ReadingMode(
    val key: String,
) {
    HORIZONTAL(key = "HORIZONTAL"),
    VERTICAL(key = "VERTICAL"),
    WEBTOON(key = "WEBTOON"),
    ;

    companion object {
        fun fromKey(key: String?): ReadingMode = entries.firstOrNull { it.key == key } ?: HORIZONTAL
    }
}
