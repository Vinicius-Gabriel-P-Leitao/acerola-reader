package br.acerola.manga.shared.util

import br.acerola.manga.shared.config.ChapterTemplateModels

fun templateToRegex(template: String = "{value}.cbz"): Regex {
    val regexStr = template
        .replace(oldValue = "{value}", newValue = "(\\d+(?:\\.\\d+)?)")
        .replace(oldValue = "{sub}", newValue = "(\\.\\d+)?") + "$"
    return Regex(pattern = regexStr, option = RegexOption.IGNORE_CASE)
}

fun detectTemplate(fileName: String): String {
    ChapterTemplateModels.presets.values.forEach { template ->
        if (templateToRegex(template).matches(input = fileName)) return template
    }

    return "{value}.cbz"
}