package br.acerola.manga.core.usecase.chapter

import br.acerola.manga.adapter.contract.ChapterPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase unificado para operações de leitura de capítulos (paginação, fluxo contínuo ou lista específica).
 */
open class ObserveChaptersUseCase<T>(
    private val chapterRepository: ChapterPort<T>
) {

    val progress: StateFlow<Int> get() = chapterRepository.progress
    val isIndexing: StateFlow<Boolean> get() = chapterRepository.isIndexing

    /**
     * Retorna o fluxo principal de capítulos para um mangá.
     */
    fun observeByManga(mangaId: Long): StateFlow<T> {
        return chapterRepository.observeChapters(mangaId)
    }

    /**
     * Retorna um fluxo para uma lista específica de identificadores de capítulos.
     */
    fun observeSpecific(mangaId: Long, chapters: List<String>): Flow<T> {
        return chapterRepository.observeSpecificChapters(mangaId, chapters)
    }

    /**
     * Carrega uma página específica de capítulos sob demanda.
     */
    suspend fun loadPage(mangaId: Long, total: Int, page: Int, pageSize: Int = 20): T {
        return chapterRepository.getChapterPage(mangaId, total, page, pageSize)
    }
}