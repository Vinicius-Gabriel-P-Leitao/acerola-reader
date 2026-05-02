package br.acerola.comic.pattern.template

enum class TemplateMacro(
    val tag: String,
) {
    CHAPTER("chapter"),
    VOLUME("volume"),
    DECIMAL("decimal"),
    EXTENSION("extension"),
    ;

    companion object {
        fun fromTag(tag: String) = entries.find { it.tag == tag }
    }
}
