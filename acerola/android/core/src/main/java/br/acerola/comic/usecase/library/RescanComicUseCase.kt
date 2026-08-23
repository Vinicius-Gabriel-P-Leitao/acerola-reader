package br.acerola.comic.usecase.library

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para rescanear quadrinhos da biblioteca.
 */
class RescanComicUseCase(
    private val comicRepository: ComicSingleSyncGateway,
) {
    val progress: StateFlow<Int> = comicRepository.progress
    val isIndexing: StateFlow<Boolean> = comicRepository.isIndexing

    suspend operator fun invoke(comicId: Long): Either<LibrarySyncError, Unit> = comicRepository.refreshManga(comicId)
}
