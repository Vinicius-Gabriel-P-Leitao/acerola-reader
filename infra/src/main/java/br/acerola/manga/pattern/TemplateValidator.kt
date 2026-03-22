package br.acerola.manga.pattern

import arrow.core.Either
import br.acerola.manga.error.message.TemplateError

enum class TemplateMacro(val tag: String) {
    VALUE("value"),
    SUB("sub"),
    EXTENSION("extension");

    companion object {
        fun fromTag(tag: String) = entries.find { it.tag == tag }
    }
}

object TemplateValidator {

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
                    return Either.Left(TemplateError.InvalidPattern("Malformed macro"))
                }

                val tag = input.substring(cursor + 1, end)
                val macro = TemplateMacro.fromTag(tag)

                if (macro == null) {
                    return Either.Left(
                        TemplateError.InvalidPattern("Invalid macro: $tag")
                    )
                }

                when (macro) {
                    TemplateMacro.VALUE -> {
                        valueCount++
                        if (valueIdx == -1) valueIdx = cursor
                    }
                    TemplateMacro.SUB -> {
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
            return Either.Left(TemplateError.InvalidPattern("Exactly one {value} is required"))
        }

        if (subCount > 1) {
            return Either.Left(TemplateError.InvalidPattern("Only one {sub} is allowed"))
        }

        if (extCount != 1) {
            return Either.Left(TemplateError.InvalidPattern("Exactly one {extension} is required"))
        }

        if (subIdx != -1 && subIdx < valueIdx) {
            return Either.Left(TemplateError.InvalidPattern("{value} must come before {sub}"))
        }
        
        if (extIdx < valueIdx) {
            return Either.Left(TemplateError.InvalidPattern("{value} must come before {extension}"))
        }

        if (subIdx != -1 && extIdx < subIdx) {
            return Either.Left(TemplateError.InvalidPattern("{sub} must come before {extension}"))
        }

        val trimmed = input.trim()
        if (!trimmed.endsWith("{${TemplateMacro.EXTENSION.tag}}")) {
            return Either.Left(TemplateError.InvalidPattern("{extension} must be at the end of the pattern"))
        }

        return Either.Right(Unit)
    }
}
