package br.acerola.comic.error.message

import br.acerola.comic.error.UserMessage
import br.acerola.comic.infra.R
import br.acerola.comic.type.UiText

sealed interface IoError : UserMessage {
    data class FileReadError(
        val path: String,
        val cause: Throwable? = null,
    ) : IoError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_file_read, args = listOf(path))
    }

    data class FileWriteError(
        val path: String,
        val cause: Throwable? = null,
    ) : IoError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_file_write, args = listOf(path))
    }

    data class FileNotFound(
        val path: String,
    ) : IoError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_file_not_found, args = listOf(path))
    }
}
