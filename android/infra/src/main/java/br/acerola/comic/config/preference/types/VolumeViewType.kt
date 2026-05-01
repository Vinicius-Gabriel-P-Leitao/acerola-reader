package br.acerola.comic.config.preference.types

enum class VolumeViewType(
    val key: String,
) {
    CHAPTER("CHAPTER"),
    VOLUME("VOLUME"),
    COVER_VOLUME("COVER_VOLUME"),
    ;

    companion object {
        fun fromKey(key: String?): VolumeViewType = entries.firstOrNull { it.key == key } ?: CHAPTER
    }
}
