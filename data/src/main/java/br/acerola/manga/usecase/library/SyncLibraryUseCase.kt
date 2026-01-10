package br.acerola.manga.usecase.library

import android.net.Uri
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.repository.port.LibrarySyncRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase responsável pela sincronização e reindexação da biblioteca de mangás.
 * Pode ser injetado com qualificadores para operar em diferentes fontes (Local/Remoto).
 */
class SyncLibraryUseCase<T>(
    private val repository: LibrarySyncRepository<T>
) {
    val progress: StateFlow<Int> = repository.progress
    val isIndexing: StateFlow<Boolean> = repository.isIndexing

    suspend fun sync(baseUri: Uri? = null): Either<LibrarySyncError, Unit> {
        return repository.syncMangas(baseUri)
    }

    suspend fun rescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> {
        return repository.rescanMangas(baseUri)
    }

    suspend fun deepRescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> {
        return repository.deepRescanLibrary(baseUri)
    }
}
