package br.acerola.manga.error.exception

sealed interface SyncError {
    data class Recoverable(val cause: Throwable) : SyncError
    data class Skipped(val cause: Throwable) : SyncError
    data class Fatal(val cause: Throwable) : SyncError
}

class SyncException(val error: SyncError) : ApplicationException()