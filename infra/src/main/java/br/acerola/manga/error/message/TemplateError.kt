package br.acerola.manga.error.message

sealed class TemplateError {
    object Duplicate : TemplateError()
    data class InvalidPattern(val reason: String) : TemplateError()
    object SystemProtected : TemplateError()
}
