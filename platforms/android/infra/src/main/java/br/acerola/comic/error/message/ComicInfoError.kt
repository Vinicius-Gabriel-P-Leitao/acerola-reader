package br.acerola.comic.error.message

import br.acerola.comic.error.UserMessage
import br.acerola.comic.infra.R
import br.acerola.comic.type.UiText

sealed interface ComicInfoError : UserMessage {
    data class InvalidXmlFormat(
        val cause: Throwable? = null,
    ) : ComicInfoError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_invalid_metadata_file)
    }

    data object MissingRootElement : ComicInfoError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_metadata_root_missing)
    }

    data class UnrecognizedMetadata(
        val fileName: String,
    ) : ComicInfoError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_unrecognized_metadata, args = listOf(fileName))
    }
}
