package br.acerola.manga.usecase.library

import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.repository.port.MangaManagementRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para rescanear mangás da biblioteca.
 */
class RescanMangaUseCase<T>(
    private val mangaRepository: MangaManagementRepository<T>
) {

    val progress: StateFlow<Int> = mangaRepository.progress
    val isIndexing: StateFlow<Boolean> = mangaRepository.isIndexing

    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return mangaRepository.refreshManga(mangaId)
    }
}