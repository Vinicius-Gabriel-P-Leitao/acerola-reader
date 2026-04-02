package br.acerola.manga.pattern

enum class TemplateMacro(val tag: String) {
    CHAPTER("chapter"),
    DECIMAL("decimal"),
    EXTENSION("extension");

    companion object {

        fun fromTag(tag: String) = entries.find { it.tag == tag }
    }
}

object ChapterTemplatePattern {
    val presets: Map<String, String> = mapOf(
        "01.*." to "{${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
        "Ch. 01.*." to "Ch. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
        "Cap. 01.*." to "Cap. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
        "chapter 01.*." to "chapter {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}"
    )

    private const val DEFAULT = "{chapter}{decimal}.*.{extension}"

    fun getTemplate(userInput: String? = null): String = userInput?.let { presets[it] ?: it } ?: DEFAULT
}
