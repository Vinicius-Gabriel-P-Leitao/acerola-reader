package br.acerola.comic.util

import br.acerola.comic.pattern.ArchiveFormatPattern
import br.acerola.comic.pattern.ChapterTemplatePattern
import br.acerola.comic.pattern.TemplateMacro

fun templateToRegex(template: String): Regex {
    val extensions = ArchiveFormatPattern.entries.joinToString("|") { it.name.lowercase() }
    val cleaned = template.replace(".+", "*").replace(".*", "*")

    val pattern =
        cleaned
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace(".", "\\.")
            .replace("{${TemplateMacro.CHAPTER.tag}}", "(\\d+)")
            .replace("{${TemplateMacro.DECIMAL.tag}}", "(?:[.,](\\d+))?")
            .replace("{${TemplateMacro.EXTENSION.tag}}", "\\.?($extensions)")
            .replace("*", ".*?")
            .replace(" ", "\\s*")

    return Regex(pattern = "^$pattern$", option = RegexOption.IGNORE_CASE)
}

fun detectTemplate(fileName: String): String {
    ChapterTemplatePattern.presets.values.forEach { template ->
        val regex = templateToRegex(template)

        if (regex.matches(input = fileName)) {
            return template
        }
    }

    return "Ch. {chapter}{decimal}.*.{extension}"
}
