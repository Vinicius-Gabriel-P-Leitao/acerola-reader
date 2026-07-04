package br.acerola.comic.service.archive

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveValidator
    @Inject
    constructor() {
        fun isPdfConversionEligible(
            targetCbzName: String,
            existingNames: Set<String>,
            chapterRegex: Regex,
        ): Boolean = !existingNames.contains(targetCbzName)

        fun isDuplicateSort(
            processedSorts: Set<String>,
            normalizedSort: String,
        ): Boolean = processedSorts.contains(normalizedSort)
    }
