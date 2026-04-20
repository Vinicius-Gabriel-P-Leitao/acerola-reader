package br.acerola.comic.adapter.contract.gateway

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface apenas para observação de dados (Read).
 */
interface ComicReadOnlyGateway<T> {
    fun observeLibrary(): Flow<List<T>> = kotlinx.coroutines.flow.flowOf(emptyList())
}

/**
 * Interface apenas para operações de sincronização e progresso (Write/Sync).
 */
interface ComicSyncGateway {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>

    suspend fun refreshManga(
        mangaId: Long,
        baseUri: Uri? = null,
    ): Either<LibrarySyncError, Unit> = Either.Right(Unit)

    suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(Unit)

    suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(Unit)

    suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(Unit)
}

/**
 * Interface completa para Engines que leem e escrevem (Diretórios, MangaDex, AniList).
 */
interface ComicGateway<T> :
    ComicReadOnlyGateway<T>,
    ComicSyncGateway

/**
 * Interface para operações de escrita na biblioteca local (ocultar/deletar).
 * Separada de MangaSyncGateway pois não são operações de sincronização.
 */
interface ComicLibraryWriteGateway {
    suspend fun hideManga(mangaId: Long): Either<LibrarySyncError, Unit>

    suspend fun deleteManga(mangaId: Long): Either<LibrarySyncError, Unit>

    suspend fun updateMangaSettings(
        mangaId: Long,
        externalSyncEnabled: Boolean,
    ): Either<LibrarySyncError, Unit>
}
