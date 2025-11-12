package br.acerola.manga.shared.config

object ChapterTemplateModels {
    val presets: Map<String, String> = mapOf(
        "Cap. 01" to "Cap. {value}.*.cbz",
        "Ch. 01" to "Ch. {value}.*.cbz",
        "chapter 01" to "chapter {value}.*.cbz",
        "num_only" to "{value}.*.cbz",

        "Cap. 01 - sub" to "Cap. {value}{sub}.*.cbz",
        "Ch. 01 - sub" to "Ch. {value}{sub}.*.cbz",
        "chapter 01 - sub" to "chapter {value}{sub}.*.cbz",
        "num_sub" to "{value}{sub}.*.cbz",

        "Ch. 01 - title" to "Ch. {value}{sub}.*.cbz",
        "Cap. 01 - title" to "Cap. {value}{sub}.*.cbz",
        "chapter 01 - title" to "chapter {value}{sub}.*.cbz"
    )

    private const val DEFAULT = "{value}.cbz"

    fun getTemplate(userInput: String? = null): String {
        return userInput?.let { presets[it] ?: it } ?: DEFAULT
    }
}
