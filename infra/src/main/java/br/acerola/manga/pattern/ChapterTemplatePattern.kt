package br.acerola.manga.pattern

object ChapterTemplatePattern {
    val presets: Map<String, String> = mapOf(
        "Cap. 01" to "Cap. {value}{sub}.*.{extension}",
        "Ch. 01" to "Ch. {value}{sub}.*.{extension}",
        "chapter 01" to "chapter {value}{sub}.*.{extension}",
        "num_only" to "{value}{sub}.*.{extension}",

        "Ch. 01 - title" to "Ch. {value}{sub}.*.{extension}",
        "Cap. 01 - title" to "Cap. {value}{sub}.*.{extension}",
        "chapter 01 - title" to "chapter {value}{sub}.*.{extension}"
    )

    private const val DEFAULT = "{value}{sub}.*.{extension}"

    fun getTemplate(userInput: String? = null): String = userInput?.let { presets[it] ?: it } ?: DEFAULT
}