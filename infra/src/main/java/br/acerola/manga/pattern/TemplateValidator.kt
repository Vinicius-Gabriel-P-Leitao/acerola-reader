package br.acerola.manga.pattern

import arrow.core.Either
import br.acerola.manga.error.message.TemplateError

// TODO: Erros devem estar em Ingles e variáveis também, tranforma validMacros em Enum
object TemplateValidator {

    private val validMacros = setOf("value", "sub", "extension")

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
                    return Either.Left(TemplateError.InvalidPattern("Macro malformada"))
                }

                val tag = input.substring(cursor + 1, end)

                if (tag !in validMacros) {
                    return Either.Left(
                        TemplateError.InvalidPattern("Macro inválida: $tag")
                    )
                }

                when (tag) {
                    "value" -> {
                        valueCount++
                        if (valueIdx == -1) valueIdx = cursor
                    }
                    "sub" -> {
                        subCount++
                        if (subIdx == -1) subIdx = cursor
                    }
                    "extension" -> {
                        extCount++
                        if (extIdx == -1) extIdx = cursor
                    }
                }

                cursor = end
            }
            cursor++
        }

        // R1: {value} é obrigatório
        if (valueCount != 1) {
            return Either.Left(TemplateError.InvalidPattern("Obrigatório conter exatamente um {value}"))
        }

        // R2: {sub} é opcional (máximo 1)
        if (subCount > 1) {
            return Either.Left(TemplateError.InvalidPattern("Apenas um {sub} é permitido"))
        }

        // R3: {extension} é obrigatório (exatamente 1)
        if (extCount != 1) {
            return Either.Left(TemplateError.InvalidPattern("Obrigatório conter exatamente um {extension}"))
        }

        // R4: Ordem - {value} deve ser a primeira macro
        if (subIdx != -1 && subIdx < valueIdx) {
            return Either.Left(TemplateError.InvalidPattern("{value} deve vir antes do {sub}"))
        }
        if (extIdx < valueIdx) {
            return Either.Left(TemplateError.InvalidPattern("{value} deve vir antes do {extension}"))
        }

        // R5: Ordem - {sub} deve vir antes do {extension}
        if (subIdx != -1 && extIdx < subIdx) {
            return Either.Left(TemplateError.InvalidPattern("{sub} deve vir antes do {extension}"))
        }

        // R6: Final da string - {extension} deve ser o sufixo final (após trim)
        val trimmed = input.trim()
        if (!trimmed.endsWith("{extension}")) {
            return Either.Left(TemplateError.InvalidPattern("{extension} deve estar obrigatoriamente no final do padrão"))
        }

        return Either.Right(Unit)
    }
}