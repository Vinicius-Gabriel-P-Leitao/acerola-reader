package br.acerola.comic.util

import br.acerola.comic.pattern.ChapterTemplatePattern
import br.acerola.comic.pattern.SpecialArchivePattern
import br.acerola.comic.pattern.VolumeTemplatePattern

enum class SortType {
    CHAPTER,
    VOLUME,
}

data class SortResult(
    val type: SortType,
    val integerPart: Int,
    val decimalPart: Int,
    val isSpecial: Boolean,
    val normalizedSort: String,
)

object SortNormalizer {

    fun normalize(
        name: String,
        type: SortType,
        fallbackIndex: Int = 0,
    ): SortResult {
        val presets =
            if (type == SortType.VOLUME) {
                VolumeTemplatePattern.presets.values
            } else {
                ChapterTemplatePattern.presets.values
            }

        presets.forEach { template ->
            val regex = templateToRegex(template)
            val match = regex.find(name)

            if (match != null) {
                val integerPart = match.groupValues[1].toInt()
                val decimalPart = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0

                return SortResult(
                    type = type,
                    integerPart = integerPart,
                    decimalPart = decimalPart,
                    isSpecial = SpecialArchivePattern.isSpecial(name),
                    normalizedSort = formatSort(integerPart, decimalPart),
                )
            }
        }

        // Fallback
        val fallbackRegex = Regex("(\\d+)(?:[.,](\\d+))?")
        val fallbackMatch = fallbackRegex.find(name)

        return if (fallbackMatch != null) {
            val integerPart = fallbackMatch.groupValues[1].toInt()
            val decimalPart = fallbackMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0

            SortResult(
                type = type,
                integerPart = integerPart,
                decimalPart = decimalPart,
                isSpecial = SpecialArchivePattern.isSpecial(name),
                normalizedSort = formatSort(integerPart, decimalPart),
            )
        } else {
            SortResult(
                type = type,
                decimalPart = 0,
                integerPart = fallbackIndex,
                normalizedSort = fallbackIndex.toString(),
                isSpecial = SpecialArchivePattern.isSpecial(name),
            )
        }
    }

    private fun formatSort(
        integer: Int,
        decimal: Int,
    ): String = if (decimal == 0) "$integer" else "$integer.$decimal"
}
