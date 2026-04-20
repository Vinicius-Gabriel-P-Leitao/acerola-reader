package br.acerola.comic.error.message

import br.acerola.comic.type.UiText

sealed class TemplateError {
    object Duplicate : TemplateError()

    data class InvalidPattern(
        val uiMessage: UiText.StringResource,
    ) : TemplateError()

    object SystemProtected : TemplateError()
}
