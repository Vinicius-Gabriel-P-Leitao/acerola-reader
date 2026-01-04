package br.acerola.manga.repository.port

import android.net.Uri
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow


interface LibraryRepository<T> {

    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    suspend fun syncMangas(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun rescanMangas(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun deepRescanLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit>

    interface MangaOperations<T> {
        fun loadMangas(): StateFlow<List<T>>
        suspend fun rescanChaptersByManga(mangaId: Long): Either<LibrarySyncError, Unit>
    }

    interface ChapterOperations<T> {
        fun loadChapterByManga(mangaId: Long): StateFlow<T>
        suspend fun loadPage(folderId: Long, total: Int, page: Int, pageSize: Int = 20): T
    }
}
