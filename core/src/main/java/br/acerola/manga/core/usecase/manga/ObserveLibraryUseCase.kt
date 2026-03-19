package br.acerola.manga.core.usecase.manga

import br.acerola.manga.adapter.port.MangaPort
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para observar a lista de mangás da biblioteca.
 */
open class ObserveLibraryUseCase<T>(
    private val mangaRepository: MangaPort<T>
) {
    val progress: StateFlow<Int> get() = mangaRepository.progress
    val isIndexing: StateFlow<Boolean> get() = mangaRepository.isIndexing

    operator fun invoke(): StateFlow<List<T>> {
        return mangaRepository.observeLibrary()
    }
}
