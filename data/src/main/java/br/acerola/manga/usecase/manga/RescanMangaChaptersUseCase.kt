package br.acerola.manga.usecase.manga

import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.repository.port.ChapterManagementRepository

/**
 * UseCase para forçar o reescaneamento de capítulos de um mangá específico.
 */
class RescanMangaChaptersUseCase<T>(
    private val chapterRepository: ChapterManagementRepository<T>
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return chapterRepository.rescanChaptersByManga(mangaId)
    }
}
