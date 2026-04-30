package br.acerola.comic.util

import br.acerola.comic.pattern.ArchiveFormatPattern
import br.acerola.comic.pattern.ChapterTemplatePattern
import br.acerola.comic.pattern.TemplateMacro
import br.acerola.comic.pattern.VolumeTemplatePattern

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
            .replace("{${TemplateMacro.VOLUME.tag}}", "(\\d+)")
            .replace("{${TemplateMacro.DECIMAL.tag}}", "(?:[.,](\\d+))?")
            .replace("{${TemplateMacro.EXTENSION.tag}}", "\\.?($extensions)")
            .replace("*", ".*?")
            .replace(" ", "\\s*")

    return Regex(pattern = "^$pattern$", option = RegexOption.IGNORE_CASE)
}

fun detectArchiveTemplate(
    name: String,
    type: SortType,
): String {
    val presets =
        if (type == SortType.VOLUME) {
            VolumeTemplatePattern.presets.values
        } else {
            ChapterTemplatePattern.presets.values
        }

    presets.forEach { template ->
        val regex = templateToRegex(template)

        if (regex.matches(input = name)) {
            return template
        }
    }

    return if (type == SortType.VOLUME) {
        VolumeTemplatePattern.getTemplate()
    } else {
        ChapterTemplatePattern.getTemplate()
    }
}
