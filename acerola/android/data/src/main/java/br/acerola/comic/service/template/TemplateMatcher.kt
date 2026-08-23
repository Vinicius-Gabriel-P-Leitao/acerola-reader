package br.acerola.comic.service.template

import br.acerola.comic.local.entity.archive.ArchiveTemplate
import br.acerola.comic.util.template.templateToRegex
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateMatcher
    @Inject
    constructor() {
        private val regexCache = ConcurrentHashMap<Long, Regex>()

        fun detect(
            fileName: String,
            templates: List<ArchiveTemplate>,
        ): ArchiveTemplate? {
            if (regexCache.size != templates.size) {
                regexCache.clear()
            }

            for (template in templates) {
                val regex =
                    regexCache.getOrPut(template.id) {
                        templateToRegex(template.pattern)
                    }
                if (regex.matches(fileName)) return template
            }
            return null
        }
    }
