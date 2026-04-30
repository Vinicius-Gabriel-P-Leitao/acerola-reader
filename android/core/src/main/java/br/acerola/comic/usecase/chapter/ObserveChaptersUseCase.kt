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
     * Retorna o fluxo principal de capítulos para um quadrinho com ordenação opcional.
     */
    fun observeByManga(
        comicId: Long,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<T> =
        chapterRepository.observeChapters(
            comicId = comicId,
            sortType = sortType,
            isAscending = isAscending,
        )

    /**
     * Carrega uma página específica de capítulos sob demanda.
     */
    suspend fun loadPage(
        comicId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): T =
        chapterRepository.getChapterPage(
            comicId = comicId,
            total = total,
            page = page,
            pageSize = pageSize,
            sortType = sortType,
            isAscending = isAscending,
        )
}
