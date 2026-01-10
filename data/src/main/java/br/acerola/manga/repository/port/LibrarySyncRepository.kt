package br.acerola.manga.repository.port

import android.net.Uri
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LibrarySyncRepository<T> {

    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    suspend fun syncMangas(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun rescanMangas(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun deepRescanLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit>
}

interface MangaManagementRepository<T> {

    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    fun loadMangas(): StateFlow<List<T>>
    suspend fun rescanManga(mangaId: Long): Either<LibrarySyncError, Unit>

}

interface ChapterManagementRepository<T> {

    fun loadChapterByManga(mangaId: Long): StateFlow<T>
    fun observeSpecificChapters(mangaId: Long, chapters: List<String>): Flow<T>
    suspend fun rescanChaptersByManga(mangaId: Long): Either<LibrarySyncError, Unit>
    suspend fun loadChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int = 20): T
}

