package br.acerola.manga.service.template

import br.acerola.manga.local.entity.archive.ChapterTemplateEntity
import br.acerola.manga.util.templateToRegex
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterTemplateMatcher @Inject constructor() {
    private val regexCache = ConcurrentHashMap<Long, Regex>()

    fun detect(fileName: String, templates: List<ChapterTemplateEntity>): ChapterTemplateEntity? {
        if (regexCache.size != templates.size) {
            regexCache.clear()
        }

        for (template in templates) {
            val regex = regexCache.getOrPut(template.id) {
                templateToRegex(template.pattern)
            }
            if (regex.matches(fileName)) return template
        }
        return null
    }
}
