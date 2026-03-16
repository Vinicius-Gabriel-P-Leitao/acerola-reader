package br.acerola.manga.error

import br.acerola.manga.types.UiText

interface UserMessage {
    val uiMessage: UiText

    data class Raw(override val uiMessage: UiText) : UserMessage {
        constructor(message: String) : this(UiText.DynamicString(message))
    }
}
