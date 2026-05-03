package br.acerola.comic.pattern.template

import arrow.core.Either
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.infra.R
import br.acerola.comic.type.UiText
import br.acerola.comic.util.sort.SortType
import br.acerola.comic.pattern.template.TemplateMacro

object TemplateValidator {
    fun validateCustomTemplate(input: String, type: SortType): Either<TemplateError, Unit> {
        var chapterCount = 0
        var volumeCount = 0
        var decimalCount = 0
        var extensionCount = 0

        var chapterIdx = -1
        var volumeIdx = -1
        var decimalIdx = -1
        var extensionIdx = -1

        var cursor = 0
        while (cursor < input.length) {
            if (input[cursor] == '{') {
                val end = input.indexOf('}', cursor)
                if (end == -1) {
                    return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_malformed_macro)))
                }

                val tag = input.substring(cursor + 1, end)
                val macro = TemplateMacro.fromTag(tag) ?: return Either.Left(
                    TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_invalid_macro, args = listOf(tag))),
                )

                when (macro) {
                    TemplateMacro.CHAPTER -> {
                        chapterCount++
                        if (chapterIdx == -1) chapterIdx = cursor
                    }
                    TemplateMacro.VOLUME -> {
                        volumeCount++
                        if (volumeIdx == -1) volumeIdx = cursor
                    }
                    TemplateMacro.DECIMAL -> {
                        decimalCount++
                        if (decimalIdx == -1) decimalIdx = cursor
                    }
                    TemplateMacro.EXTENSION -> {
                        extensionCount++
                        if (extensionIdx == -1) extensionIdx = cursor
                    }
                }

                cursor = end
            }
            cursor++
        }

        // Validação do marcador principal obrigatório (Chapter ou Volume)
        val mainValidation = when (type) {
            SortType.CHAPTER -> validateChapter(chapterCount, chapterIdx, decimalIdx, extensionIdx, extensionCount, input)
            SortType.VOLUME -> validateVolume(volumeCount, volumeIdx, extensionIdx)
        }

        if (mainValidation is Either.Left) return mainValidation

        // Validações comuns
        if (decimalCount > 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_decimal_duplicate)))
        }

        return Either.Right(Unit)
    }

    private fun validateChapter(count: Int, idx: Int, decimalIdx: Int, extensionIdx: Int, extensionCount: Int, input: String): Either<TemplateError, Unit> {
        if (count != 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_required)))
        }
        if (extensionCount != 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_extension_required)))
        }
        if (decimalIdx != -1 && decimalIdx < idx) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_before_decimal)))
        }
        if (extensionIdx != -1 && extensionIdx < idx) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_before_extension)))
        }

        val trimmed = input.trim()
        if (!trimmed.endsWith("{${TemplateMacro.EXTENSION.tag}}")) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_extension_at_end)))
        }

        return Either.Right(Unit)
    }

    private fun validateVolume(count: Int, idx: Int, extensionIdx: Int): Either<TemplateError, Unit> {
        if (count != 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_volume_required)))
        }
        if (extensionIdx != -1 && extensionIdx < idx) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_before_extension))) // Reutilizando erro de ordem
        }
        return Either.Right(Unit)
    }
}
