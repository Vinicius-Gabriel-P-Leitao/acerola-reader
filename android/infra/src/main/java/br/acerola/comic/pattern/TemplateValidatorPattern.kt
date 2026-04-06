package br.acerola.comic.pattern

import arrow.core.Either
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.infra.R
import br.acerola.comic.type.UiText

object TemplateValidatorPattern {

    fun validateCustomTemplate(input: String): Either<TemplateError, Unit> {
        var valueCount = 0
        var subCount = 0
        var extCount = 0

        var valueIdx = -1
        var subIdx = -1
        var extIdx = -1

        var cursor = 0
        while (cursor < input.length) {
            if (input[cursor] == '{') {
                val end = input.indexOf('}', cursor)
                if (end == -1) {
                    return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_malformed_macro)))
                }

                val tag = input.substring(cursor + 1, end)
                val macro = TemplateMacro.fromTag(tag)
                    ?: return Either.Left(
                        TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_invalid_macro, args = listOf(tag)))
                    )

                when (macro) {
                    TemplateMacro.CHAPTER -> {
                        valueCount++
                        if (valueIdx == -1) valueIdx = cursor
                    }
                    TemplateMacro.DECIMAL -> {
                        subCount++
                        if (subIdx == -1) subIdx = cursor
                    }
                    TemplateMacro.EXTENSION -> {
                        extCount++
                        if (extIdx == -1) extIdx = cursor
                    }
                }

                cursor = end
            }
            cursor++
        }

        if (valueCount != 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_required)))
        }

        if (subCount > 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_decimal_duplicate)))
        }

        if (extCount != 1) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_extension_required)))
        }

        if (subIdx != -1 && subIdx < valueIdx) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_before_decimal)))
        }

        if (extIdx < valueIdx) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_chapter_before_extension)))
        }

        if (subIdx != -1 && extIdx < subIdx) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_decimal_before_extension)))
        }

        val trimmed = input.trim()
        if (!trimmed.endsWith("{${TemplateMacro.EXTENSION.tag}}")) {
            return Either.Left(TemplateError.InvalidPattern(UiText.StringResource(R.string.error_template_extension_at_end)))
        }

        return Either.Right(Unit)
    }
}
