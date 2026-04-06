package br.acerola.comic.error

import br.acerola.comic.type.UiText

interface UserMessage {
    val uiMessage: UiText

    data class Raw(override val uiMessage: UiText) : UserMessage {
        constructor(message: String) : this(UiText.DynamicString(message))
    }
}
