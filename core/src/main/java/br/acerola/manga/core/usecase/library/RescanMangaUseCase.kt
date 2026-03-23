package br.acerola.manga.core.usecase.library

import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.adapter.contract.gateway.MangaSyncGateway
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para rescanear mangás da biblioteca.
 */
class RescanMangaUseCase(
    private val mangaRepository: MangaSyncGateway
) {

    val progress: StateFlow<Int> = mangaRepository.progress
    val isIndexing: StateFlow<Boolean> = mangaRepository.isIndexing

    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return mangaRepository.refreshManga(mangaId)
    }
}