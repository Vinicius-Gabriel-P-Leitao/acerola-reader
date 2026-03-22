package br.acerola.manga.adapter.contract

import android.net.Uri
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

interface MangaPort<T> {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    fun observeLibrary(): StateFlow<List<T>>
    suspend fun refreshManga(mangaId: Long, baseUri: Uri? = null): Either<LibrarySyncError, Unit>
    suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit>
    suspend fun updateMangaSettings(mangaId: Long, externalSyncEnabled: Boolean): Either<LibrarySyncError, Unit> = Either.Right(Unit)
}
