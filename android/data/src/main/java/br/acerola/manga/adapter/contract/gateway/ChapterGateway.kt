package br.acerola.manga.adapter.contract.gateway

import android.net.Uri
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChapterGateway<T> {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    fun observeChapters(mangaId: Long): StateFlow<T>
    fun observeSpecificChapters(mangaId: Long, chapters: List<String>): Flow<T>

    suspend fun refreshMangaChapters(mangaId: Long, baseUri: Uri? = null): Either<LibrarySyncError, Unit>
    suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int = 20): T
}
