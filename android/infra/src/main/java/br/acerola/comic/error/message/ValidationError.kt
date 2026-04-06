package br.acerola.comic.error.message

import br.acerola.comic.error.UserMessage
import br.acerola.comic.infra.R
import br.acerola.comic.type.UiText

sealed interface ValidationError : UserMessage {
    data object EmptyField : ValidationError {
        override val uiMessage = UiText.StringResource(resId = R.string.validation_empty_field)
    }

    data class InvalidFormat(val field: String) : ValidationError {
        override val uiMessage = UiText.StringResource(
            resId = R.string.validation_invalid_format, args = listOf(field)
        )
    }

    data class OutOfRange(
        val field: String, val min: Int, val max: Int, val actual: Int
    ) : ValidationError {
        override val uiMessage = UiText.StringResource(
            resId = R.string.validation_out_of_range, args = listOf(field, min, max, actual)
        )
    }

    data class TooShort(val field: String, val minLength: Int) : ValidationError {
        override val uiMessage = UiText.StringResource(
            resId = R.string.validation_too_short, args = listOf(field, minLength)
        )
    }

    data class TooLong(val field: String, val maxLength: Int) : ValidationError {
        override val uiMessage = UiText.StringResource(
            resId = R.string.validation_too_long, args = listOf(field, maxLength)
        )
    }
}
