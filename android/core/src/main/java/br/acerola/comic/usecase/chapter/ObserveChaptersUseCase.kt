package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase genérico para operações simples de capítulos.
 * Mantido para compatibilidade com Reader e outras ViewModels.
 */
open class ObserveChaptersUseCase<T>(
    private val chapterRepository: ChapterGateway<T>,
) {
    val progress: StateFlow<Int> get() = chapterRepository.progress
    val isIndexing: StateFlow<Boolean> get() = chapterRepository.isIndexing

    fun observeByComic(
        comicId: Long,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<T> = chapterRepository.observeChapters(comicId, sortType, isAscending)

    suspend fun loadPage(
        comicId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): T = chapterRepository.getChapterPage(comicId, total, page, pageSize, sortType, isAscending)
}

