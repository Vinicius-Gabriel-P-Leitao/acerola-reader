package br.acerola.manga.core.usecase.library

import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase para forçar o reescaneamento de capítulos de um mangá específico.
 */
class RescanMangaChaptersUseCase<T>(
    private val chapterRepository: ChapterGateway<T>
) {
    val progress: StateFlow<Int> = chapterRepository.progress
    val isIndexing: StateFlow<Boolean> = chapterRepository.isIndexing

    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return chapterRepository.refreshMangaChapters(mangaId)
    }
}
