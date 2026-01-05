package br.acerola.manga.error.message

import br.acerola.manga.error.UserMessage
import br.acerola.manga.infrastructure.R
import br.acerola.manga.types.UiText

sealed interface NetworkError : UserMessage {
    data class RateLimitExceeded(
        val retryAfter: Long? = null,
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_http_error_rate_limit)
    }

    data class ConnectionFailed(
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_network_error)
    }

    data class Timeout(
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_timeout)
    }

    data class ServerError(
        val code: Int,
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_server, args = listOf(code))
    }

    data class Unauthorized(
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_unauthorized)
    }

    data class NotFound(
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_not_found)
    }

    data class HttpError(
        val code: Int,
        val cause: Throwable? = null
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_http, args = listOf(code))
    }

    data class UnexpectedError(
        val cause: Throwable
    ) : NetworkError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_generic_internal_error)
    }
}