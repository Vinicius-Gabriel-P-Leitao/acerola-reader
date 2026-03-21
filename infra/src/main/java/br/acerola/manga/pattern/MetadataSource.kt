package br.acerola.manga.pattern

enum class MetadataSource(val source: String) {
    COMIC_INFO(source = "comic_info"),
    MANGADEX(source = "mangadex"),
    ANILIST(source = "anilist"),
    MANUAL(source = "manual");

    companion object {
        fun from(value: String?): MetadataSource? =
            entries.find { it.source.equals(value, ignoreCase = true) }
    }
}
