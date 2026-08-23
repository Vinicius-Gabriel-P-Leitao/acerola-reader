package br.acerola.comic.config.preference.types

enum class HomeLayoutType(
    val key: String,
) {
    LIST(key = "LIST"),
    GRID(key = "GRID"),
    ;

    companion object {
        fun fromKey(key: String?): HomeLayoutType = entries.firstOrNull { it.key == key } ?: LIST
    }
}
