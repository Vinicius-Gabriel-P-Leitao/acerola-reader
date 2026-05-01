package br.acerola.comic.util.template

import br.acerola.comic.pattern.archive.ArchiveFormat
import br.acerola.comic.pattern.template.TemplateMacro

fun templateToRegex(template: String): Regex {
    val extensions = ArchiveFormat.entries.joinToString("|") { it.name.lowercase() }
    val cleaned = template.replace(".+", "*").replace(".*", "*")

    val pattern =
        cleaned
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace(".", "\\.")
            .replace("{${TemplateMacro.CHAPTER.tag}}", "(\\d+)")
            .replace("{${TemplateMacro.VOLUME.tag}}", "(\\d+)")
            .replace("{${TemplateMacro.DECIMAL.tag}}", "(?:[.,](\\d+))?")
            .replace("{${TemplateMacro.EXTENSION.tag}}", "\\.?($extensions)")
            .replace("*", ".*?")
            .replace(" ", "\\s*")

    return Regex(pattern = "^$pattern$", option = RegexOption.IGNORE_CASE)
}

fun detectArchiveTemplate(
    name: String,
    templates: List<String>,
    fallbackTemplate: String,
): String {
    templates.forEach { template ->
        val regex = templateToRegex(template)

        if (regex.matches(input = name)) {
            return template
        }
    }

    return fallbackTemplate
}
