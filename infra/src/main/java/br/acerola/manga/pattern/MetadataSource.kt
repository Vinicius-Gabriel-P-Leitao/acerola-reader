package br.acerola.manga.pattern

enum class MetadataSource(val source: String, val displayName: String) {
    COMIC_INFO(source = "comic_info", displayName = "COMIC_INFO"),
    MANGADEX(source = "mangadex", displayName = "MANGADEX"),
    ANILIST(source = "anilist", displayName = "ANILIST");

    companion object {
        fun from(value: String?): MetadataSource? =
            entries.find { it.source.equals(value, ignoreCase = true) }
    }
}
