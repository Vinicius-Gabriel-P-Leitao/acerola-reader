package br.acerola.manga.error

import br.acerola.manga.error.UserMessage
import br.acerola.manga.infrastructure.R
import br.acerola.manga.types.UiText

sealed interface LibrarySyncError : UserMessage {
    data class FolderAccessDenied(val cause: Throwable? = null) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(R.string.description_file_system_access_error)
    }

    data class DiskIOFailure(
        val path: String,
        val cause: Throwable? = null
    ) : LibrarySyncError {
        // Reusing file system access error for now, maybe create specific string later
        override val uiMessage = UiText.StringResource(R.string.description_file_system_access_error)
    }

    data class DatabaseError(
        val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(R.string.description_dao_error)
    }

    data class NetworkError(
        val cause: Throwable? = null
    ) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(R.string.description_network_error)
    }

    data class UnexpectedError(val cause: Throwable) : LibrarySyncError {
        override val uiMessage = UiText.StringResource(R.string.description_generic_internal_error)
    }
}
