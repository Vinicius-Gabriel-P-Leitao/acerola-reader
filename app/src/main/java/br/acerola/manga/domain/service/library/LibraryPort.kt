package br.acerola.manga.domain.service.library

import android.net.Uri
import androidx.annotation.Nullable
import kotlinx.coroutines.flow.StateFlow


interface LibraryPort<T> {

    val progress: StateFlow<Int>

    suspend fun syncMangas(baseUri: Uri?)
    suspend fun rescanMangas(baseUri: Uri?)
    suspend fun deepRescanLibrary(baseUri: Uri?)

    interface MangaOperations<T> {
        fun loadMangas(): StateFlow<List<T>>
        suspend fun rescanChaptersByManga(mangaId: Long)
    }

    interface ChapterOperations<T> {
        fun loadChapterByManga(mangaId: Long): StateFlow<T>
        suspend fun loadNextPage(folderId: Long, total: Int, page: Int, pageSize: Int = 20): T
    }
}
