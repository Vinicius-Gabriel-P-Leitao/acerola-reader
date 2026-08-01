package br.acerola.comic.config.preference.types

enum class AppTheme(
    val key: String,
) {
    DYNAMIC(key = "dynamic"),
    CATPPUCCIN(key = "catppuccin"),
    DRACULA(key = "dracula"),
    NORD(key = "nord"),
    TOKYO_NIGHT(key = "tokyo_night"),
    ;

    companion object {
        fun fromKey(key: String?): AppTheme = entries.find { it.key == key } ?: CATPPUCCIN
    }
}
