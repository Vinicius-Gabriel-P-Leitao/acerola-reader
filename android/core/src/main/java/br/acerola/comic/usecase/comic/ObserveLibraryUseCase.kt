package br.acerola.comic.usecase.comic

import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicSyncStatusGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UseCase para observar a lista de quadrinhos da biblioteca.
 */
open class ObserveLibraryUseCase<T>(
    private val syncGateway: ComicSyncStatusGateway? = null,
    private val comicRepository: ComicReadOnlyGateway<T>,
) {
    val progress: Flow<Int> get() = syncGateway?.progress ?: MutableStateFlow(-1).asStateFlow()
    val isIndexing: Flow<Boolean> get() = syncGateway?.isIndexing ?: MutableStateFlow(false).asStateFlow()

    operator fun invoke(): Flow<List<T>> = comicRepository.observeLibrary()
}
