package br.acerola.comic.usecase.comic

import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicSyncGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UseCase para observar a lista de mangás da biblioteca.
 */
open class ObserveLibraryUseCase<T>(
    private val syncGateway: ComicSyncGateway? = null,
    private val mangaRepository: ComicReadOnlyGateway<T>,
) {
    val progress: StateFlow<Int> get() = syncGateway?.progress ?: MutableStateFlow(-1).asStateFlow()
    val isIndexing: StateFlow<Boolean> get() = syncGateway?.isIndexing ?: MutableStateFlow(false).asStateFlow()

    operator fun invoke(): Flow<List<T>> {
        return mangaRepository.observeLibrary()
    }
}
