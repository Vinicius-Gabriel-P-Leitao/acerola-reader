package br.acerola.comic.usecase.library

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicSyncGateway
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para rescanear mangás da biblioteca.
 */
class RescanComicUseCase(
    private val mangaRepository: ComicSyncGateway
) {

    val progress: StateFlow<Int> = mangaRepository.progress
    val isIndexing: StateFlow<Boolean> = mangaRepository.isIndexing

    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return mangaRepository.refreshManga(mangaId)
    }
}