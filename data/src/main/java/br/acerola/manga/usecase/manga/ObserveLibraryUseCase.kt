package br.acerola.manga.usecase.manga

import br.acerola.manga.repository.port.MangaManagementRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para observar a lista de mangás da biblioteca.
 */
open class ObserveLibraryUseCase<T>(
    private val mangaRepository: MangaManagementRepository<T>
) {
    val progress: StateFlow<Int> get() = mangaRepository.progress
    val isIndexing: StateFlow<Boolean> get() = mangaRepository.isIndexing

    operator fun invoke(): StateFlow<List<T>> {
        return mangaRepository.observeLibrary()
    }
}
