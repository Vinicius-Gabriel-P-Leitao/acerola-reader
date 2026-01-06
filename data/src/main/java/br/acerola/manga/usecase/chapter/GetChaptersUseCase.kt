package br.acerola.manga.usecase.chapter

import br.acerola.manga.repository.port.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase unificado para operações de leitura de capítulos (paginação, fluxo contínuo ou lista específica).
 */
class GetChaptersUseCase<T>(
    private val chapterOperations: LibraryRepository.ChapterOperations<T>
) {
    /**
     * Retorna o fluxo principal de capítulos para um mangá.
     */
    fun observeByManga(mangaId: Long): StateFlow<T> {
        return chapterOperations.loadChapterByManga(mangaId)
    }

    /**
     * Retorna um fluxo para uma lista específica de identificadores de capítulos.
     */
    fun observeSpecific(mangaId: Long, chapters: List<String>): Flow<T> {
        return chapterOperations.observeSpecificChapters(mangaId, chapters)
    }

    /**
     * Carrega uma página específica de capítulos sob demanda.
     */
    suspend fun loadPage(mangaId: Long, total: Int, page: Int, pageSize: Int = 20): T {
        return chapterOperations.loadChapterPage(mangaId, total, page, pageSize)
    }
}
