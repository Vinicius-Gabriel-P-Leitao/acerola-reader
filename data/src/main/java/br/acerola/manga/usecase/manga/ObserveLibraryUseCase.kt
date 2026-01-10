package br.acerola.manga.usecase.manga

import br.acerola.manga.repository.port.MangaManagementRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para observar a lista de mangás da biblioteca.
 */
class ObserveLibraryUseCase<T>(
    private val mangaRepository: MangaManagementRepository<T>
) {

    val progress: StateFlow<Int> = mangaRepository.progress
    val isIndexing: StateFlow<Boolean> = mangaRepository.isIndexing

    operator fun invoke(): StateFlow<List<T>> {
        return mangaRepository.loadMangas()
    }
}
