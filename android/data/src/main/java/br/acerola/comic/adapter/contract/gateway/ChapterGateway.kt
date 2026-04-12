package br.acerola.comic.adapter.contract.gateway

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

interface ChapterGateway<T> {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    fun observeChapters(mangaId: Long): StateFlow<T>

    suspend fun refreshComicChapters(mangaId: Long, baseUri: Uri? = null): Either<LibrarySyncError, Unit>
    suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int = 20): T
}
