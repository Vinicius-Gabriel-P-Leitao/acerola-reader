package br.acerola.manga.config.preference

enum class AppTheme(val key: String) {
    CATPPUCCIN(key = "catppuccin"),
    DYNAMIC(key = "dynamic"),
    DRACULA(key = "dracula"),
    NORD(key = "nord");

    companion object {
        fun fromKey(key: String?): AppTheme = entries.find { it.key == key } ?: CATPPUCCIN
    }
}
