package br.acerola.comic.usecase.library

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UseCase responsável pela sincronização e reindexação da biblioteca de quadrinhos.
 * Pode ser injetado com qualificadores para operar em diferentes fontes (Local/Remoto).
 */
class SyncLibraryUseCase(
    private val scanGateway: ComicLibraryScanGateway? = null,
    private val rebuildGateway: ComicRebuildGateway? = null,
) {
    val progress: StateFlow<Int> =
        scanGateway?.progress ?: rebuildGateway?.progress ?: MutableStateFlow(-1).asStateFlow()
    val isIndexing: StateFlow<Boolean> =
        scanGateway?.isIndexing ?: rebuildGateway?.isIndexing ?: MutableStateFlow(false).asStateFlow()

    suspend fun sync(baseUri: Uri? = null): Either<LibrarySyncError, Unit> =
        scanGateway?.incrementalScan(baseUri) ?: Either.Left(LibrarySyncError.UnexpectedError(Exception("Sync not supported")))

    suspend fun rescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> =
        scanGateway?.refreshLibrary(baseUri) ?: Either.Left(LibrarySyncError.UnexpectedError(Exception("Rescan not supported")))

    suspend fun deepRescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> =
        rebuildGateway?.rebuildLibrary(baseUri) ?: Either.Left(LibrarySyncError.UnexpectedError(Exception("Deep rescan not supported")))
}
