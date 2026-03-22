package br.acerola.manga.pattern

object ChapterTemplatePattern {
    val presets: Map<String, String> = mapOf(
        "num_only" to "{value}{sub}.*.{extension}",
        "Ch. 01 - title" to "Ch. {value}{sub}.*.{extension}",
        "Cap. 01 - title" to "Cap. {value}{sub}.*.{extension}",
        "chapter 01 - title" to "chapter {value}{sub}.*.{extension}"
    )

    private const val DEFAULT = "{value}{sub}.*.{extension}"

    fun getTemplate(userInput: String? = null): String = userInput?.let { presets[it] ?: it } ?: DEFAULT
}