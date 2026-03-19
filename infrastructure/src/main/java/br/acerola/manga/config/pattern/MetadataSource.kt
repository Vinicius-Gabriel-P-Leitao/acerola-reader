package br.acerola.manga.config.pattern

enum class MetadataSource(val source: String) {
    COMIC_INFO(source = "comic_info"),
    MANGADEX(source = "mangadex"),
    // TODO: Adicionar Anilist
    MANUAL(source = "manual");

    companion object {
        fun from(value: String?): MetadataSource =
            entries.find { it.source.equals(value, ignoreCase = true) } ?: MANGADEX
    }
}