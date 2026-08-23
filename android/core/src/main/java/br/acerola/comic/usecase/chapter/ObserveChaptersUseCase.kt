package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase genérico para operações simples de capítulos.
 * Mantido para compatibilidade com Reader e outras ViewModels.
 */
open class ObserveChaptersUseCase<T>(
    private val readGateway: ChapterReadGateway<T>,
    private val syncStatusGateway: ChapterSyncStatusGateway,
) {
    val progress: Flow<Int> get() = syncStatusGateway.progress
    val isIndexing: Flow<Boolean> get() = syncStatusGateway.isIndexing

    fun observeByComic(
        comicId: Long,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<T> = readGateway.observeChapters(comicId, sortType, isAscending)

    suspend fun loadPage(
        comicId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): T = readGateway.getChapterPage(comicId, total, page, pageSize, sortType, isAscending)
}
