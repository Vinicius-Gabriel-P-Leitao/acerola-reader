package br.acerola.manga.util

import br.acerola.manga.pattern.ArchiveFormatPattern
import br.acerola.manga.pattern.ChapterTemplatePattern

fun templateToRegex(template: String): Regex {
    val extensions = ArchiveFormatPattern.entries.joinToString("|") { it.name.lowercase() }
    val cleaned = template.replace(".+", "*").replace(".*", "*")
    val pattern = cleaned
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("[", "\\[")
        .replace("]", "\\]").replace(".", "\\.")
        .replace("{value}", "(\\d+)")
        .replace("{sub}", "(?:[.,](\\d+))?")
        .replace("{extension}", "\\.?($extensions)")
        .replace("*", ".*?").replace(" ", "\\s*")

    return Regex(pattern = "^$pattern$", option = RegexOption.IGNORE_CASE)
}

fun detectTemplate(fileName: String): String {
    ChapterTemplatePattern.presets.values.forEach { template ->
        val regex = templateToRegex(template)

        if (regex.matches(input = fileName)) {
            return template
        }
    }

    return "Ch. {value}{sub}.*.{extension}"
}
