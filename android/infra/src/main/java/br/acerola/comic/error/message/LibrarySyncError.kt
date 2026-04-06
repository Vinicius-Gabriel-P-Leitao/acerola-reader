package br.acerola.comic.error.message

import br.acerola.comic.error.UserMessage
import br.acerola.comic.error.exception.MangadexRequestException
import br.acerola.comic.infra.R
import br.acerola.comic.type.UiText

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

    data class SyncNetworkError(
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

    data class MetadataNotFound(
        val source: String,
        val identifier: String
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_metadata_not_found, args = listOf(source, identifier))
    }

    data class RemoteNetworkError(
        val error: NetworkError
    ) : LibrarySyncError {
        override val uiMessage = error.uiMessage
    }

    data object ExternalSyncDisabled : LibrarySyncError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_sync_disabled)
    }
}
