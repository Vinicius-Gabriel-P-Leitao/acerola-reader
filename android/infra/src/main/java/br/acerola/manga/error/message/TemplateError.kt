package br.acerola.manga.error.message

import br.acerola.manga.type.UiText

sealed class TemplateError {
    object Duplicate : TemplateError()
    data class InvalidPattern(val uiMessage: UiText.StringResource) : TemplateError()
    object SystemProtected : TemplateError()
}
