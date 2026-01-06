package br.acerola.manga.config.pattern

object ChapterTemplatePattern {
    val presets: Map<String, String> = mapOf(
        "Cap. 01" to "Cap. {value}{sub}.*.cbz",
        "Ch. 01" to "Ch. {value}{sub}.*.cbz",
        "chapter 01" to "chapter {value}{sub}.*.cbz",
        "num_only" to "{value}{sub}.*.cbz",

        "Ch. 01 - title" to "Ch. {value}{sub}.*.cbz",
        "Cap. 01 - title" to "Cap. {value}{sub}.*.cbz",
        "chapter 01 - title" to "chapter {value}{sub}.*.cbz"
    )

    private const val DEFAULT = "{value}{sub}.*.cbz"

    fun getTemplate(userInput: String? = null): String = userInput?.let { presets[it] ?: it } ?: DEFAULT
}