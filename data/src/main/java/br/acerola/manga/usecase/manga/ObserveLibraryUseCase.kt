package br.acerola.manga.usecase.manga

import br.acerola.manga.repository.port.LibraryRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para observar a lista de mangás da biblioteca.
 */
class ObserveLibraryUseCase<T>(
    private val mangaOperations: LibraryRepository.MangaOperations<T>
) {
    val progress: StateFlow<Int> = mangaOperations.progress
    val isIndexing: StateFlow<Boolean> = mangaOperations.isIndexing

    operator fun invoke(): StateFlow<List<T>> {
        return mangaOperations.loadMangas()
    }
}
