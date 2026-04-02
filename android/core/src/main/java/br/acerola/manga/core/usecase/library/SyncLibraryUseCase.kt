package br.acerola.manga.core.usecase.library

import android.net.Uri
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.adapter.contract.gateway.MangaSyncGateway
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase responsável pela sincronização e reindexação da biblioteca de mangás.
 * Pode ser injetado com qualificadores para operar em diferentes fontes (Local/Remoto).
 *
 * Agora depende diretamente de [MangaSyncGateway].
 */
class SyncLibraryUseCase(
    private val repository: MangaSyncGateway
) {
    val progress: StateFlow<Int> = repository.progress
    val isIndexing: StateFlow<Boolean> = repository.isIndexing

    suspend fun sync(baseUri: Uri? = null): Either<LibrarySyncError, Unit> {
        return repository.incrementalScan(baseUri)
    }

    suspend fun rescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> {
        return repository.refreshLibrary(baseUri)
    }

    suspend fun deepRescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> {
        return repository.rebuildLibrary(baseUri)
    }
}
