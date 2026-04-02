package br.acerola.manga.error.message

import br.acerola.manga.error.UserMessage
import br.acerola.manga.infra.R
import br.acerola.manga.type.UiText

sealed interface ChapterError : UserMessage {
    data class ArchiveNotFound(val path: String) : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_archive_not_found, args = listOf(path))
    }

    data class ArchiveCorrupted(
        val path: String, val cause: Throwable? = null
    ) : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_archive_corrupted, args = listOf(path))
    }

    data class ExtractionFailed(
        val cause: Throwable? = null
    ) : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_extraction_failed)
    }

    data object StoragePermissionDenied : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_storage_permission)
    }

    data class DatabaseError(
        val cause: Throwable? = null
    ) : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_database)
    }

    data class InvalidChapterData(val reason: String) : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_invalid_chapter, args = listOf(reason))
    }

    data class UnexpectedError(
        val cause: Throwable
    ) : ChapterError {
        override val uiMessage = UiText.StringResource(resId = R.string.error_unexpected)
    }
}
