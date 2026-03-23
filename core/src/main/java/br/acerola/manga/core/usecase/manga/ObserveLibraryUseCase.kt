package br.acerola.manga.core.usecase.manga

import br.acerola.manga.adapter.contract.gateway.MangaReadOnlyGateway
import br.acerola.manga.adapter.contract.gateway.MangaSyncGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UseCase para observar a lista de mangás da biblioteca.
 */
open class ObserveLibraryUseCase<T>(
    private val syncGateway: MangaSyncGateway? = null,
    private val mangaRepository: MangaReadOnlyGateway<T>,
) {
    val progress: StateFlow<Int> get() = syncGateway?.progress ?: MutableStateFlow(-1).asStateFlow()
    val isIndexing: StateFlow<Boolean> get() = syncGateway?.isIndexing ?: MutableStateFlow(false).asStateFlow()

    operator fun invoke(): Flow<List<T>> {
        return mangaRepository.observeLibrary()
    }

    suspend fun updateMangaSettings(mangaId: Long, externalSyncEnabled: Boolean) =
        syncGateway?.updateMangaSettings(mangaId, externalSyncEnabled)
}
