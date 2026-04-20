package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase unificado para operações de leitura de capítulos (paginação, fluxo contínuo ou lista específica).
 */
open class ObserveChaptersUseCase<T>(
    private val chapterRepository: ChapterGateway<T>,
) {
    val progress: StateFlow<Int> get() = chapterRepository.progress
    val isIndexing: StateFlow<Boolean> get() = chapterRepository.isIndexing

    /**
     * Retorna o fluxo principal de capítulos para um mangá.
     */
    fun observeByManga(mangaId: Long): StateFlow<T> = chapterRepository.observeChapters(mangaId)

    /**
     * Carrega uma página específica de capítulos sob demanda.
     */
    suspend fun loadPage(
        mangaId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
    ): T = chapterRepository.getChapterPage(mangaId, total, page, pageSize)
}
