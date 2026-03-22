package br.acerola.manga.error.message

import br.acerola.manga.error.UserMessage
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.infra.R
import br.acerola.manga.type.UiText

sealed interface LibrarySyncError : UserMessage {
    data class FolderAccessDenied(
        val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_file_system_access_error)
    }

    data class DiskIOFailure(
        val path: String, val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_file_system_access_error)
    }

    data class DatabaseError(
        val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_dao_error)
    }

    data class NetworkError(
        val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_network_error)
    }

    data class MangadexError(
        val cause: MangadexRequestException
    ): LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = cause.description)
    }

    data class MalformedLibrary(
        val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_malformed_library)
    }

    data class UnexpectedError(
        val cause: Throwable
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.description_generic_internal_error)
    }

    data object ExternalSyncDisabled : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_sync_disabled)
    }
}
