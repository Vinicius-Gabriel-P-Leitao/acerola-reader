package br.acerola.comic.adapter.contract.gateway

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

interface ChapterGateway<T> {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    fun observeChapters(
        comicId: Long,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<T>

    suspend fun refreshComicChapters(
        comicId: Long,
        baseUri: Uri? = null,
    ): Either<LibrarySyncError, Unit>

    suspend fun getChapterPage(
        comicId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): T
}
