package br.acerola.manga.local.database.entity.metadata

enum class MetadataSource(val source: String) {
    MANGADEX(source = "mangadex"),
    COMIC_INFO(source = "comic_info"),
    MANUAL(source = "manual");

    companion object {
        fun from(value: String?): MetadataSource =
            entries.find { it.source.equals(value, ignoreCase = true) } ?: MANGADEX
    }
}
