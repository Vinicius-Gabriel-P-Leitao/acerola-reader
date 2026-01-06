package br.acerola.manga.util

import br.acerola.manga.config.pattern.ChapterTemplatePattern

fun templateToRegex(template: String): Regex {
    val cleaned = template.replace(oldValue = ".+", newValue = "*").replace(oldValue = ".*", newValue = "*")

    val pattern = cleaned
        .replace(oldValue = "(", newValue = "\\(").replace(oldValue = ")", newValue = "\\)")
        .replace(oldValue = "[", newValue = "\\[").replace(oldValue = "]", newValue = "\\]")
        .replace(oldValue = ".", newValue = "\\.")
        .replace(oldValue = "{value}", newValue = "(\\d+)")
        .replace(oldValue = "{sub}", newValue = "(?:[.,](\\d+))?")
        .replace(oldValue = "*", newValue = ".*?")
        .replace(oldValue = " ", newValue = "\\s*")

    return Regex(pattern = "^$pattern$", option = RegexOption.IGNORE_CASE)
}

fun detectTemplate(fileName: String): String {
    ChapterTemplatePattern.presets.values.forEach { template ->
        val regex = templateToRegex(template)
        if (regex.matches(input = fileName)) {
            return template
        }
    }

    return "Ch. {value}{sub}.*.cbz"
}