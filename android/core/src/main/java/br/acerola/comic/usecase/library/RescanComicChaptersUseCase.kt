package br.acerola.comic.usecase.library

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para forçar o reescaneamento de capítulos de um mangá específico.
 */
class RescanComicChaptersUseCase<T>(
    private val chapterRepository: ChapterGateway<T>
) {
    val progress: StateFlow<Int> = chapterRepository.progress
    val isIndexing: StateFlow<Boolean> = chapterRepository.isIndexing

    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return chapterRepository.refreshComicChapters(mangaId)
    }
}
