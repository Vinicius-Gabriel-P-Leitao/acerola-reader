package br.acerola.manga.config.preference

enum class AppTheme(val key: String) {
    DYNAMIC(key = "dynamic"),
    CATPPUCCIN(key = "catppuccin"),
    DRACULA(key = "dracula"),
    NORD(key = "nord");

    companion object {
        fun fromKey(key: String?): AppTheme = entries.find { it.key == key } ?: CATPPUCCIN
    }
}
