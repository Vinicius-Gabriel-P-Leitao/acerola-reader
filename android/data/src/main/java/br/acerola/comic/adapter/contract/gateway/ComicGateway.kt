package br.acerola.comic.adapter.contract.gateway

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface para observação do estado de sincronização.
 */
interface ComicSyncStatusGateway {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>
}

/**
 * Interface para sincronização de um único item.
 * Todas as Engines implementam isso - refreshManga é universalmente suportado.
 */
interface ComicSingleSyncGateway : ComicSyncStatusGateway {
    suspend fun refreshManga(
        comicId: Long,
        baseUri: Uri? = null,
    ): Either<LibrarySyncError, Unit>
}

/**
 * Interface para operações de varredura da biblioteca (Incremental e Completa).
 * Implementada por Directory, MangaDex e AniList.
 */
interface ComicLibraryScanGateway : ComicSyncStatusGateway {
    suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit>

    suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit>
}

/**
 * Interface para reconstrução profunda da biblioteca.
 * Implementada apenas pela ComicDirectoryEngine.
 */
interface ComicRebuildGateway : ComicSyncStatusGateway {
    suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit>
}

/**
 * Interface apenas para observação de dados (Read).
 */
interface ComicReadOnlyGateway<T> {
    fun observeLibrary(): Flow<List<T>> = kotlinx.coroutines.flow.flowOf(emptyList())
}

/**
 * Interface agregada para sincronização completa.
 * Engines que suportam todas as operações implementam esta interface.
 */
interface ComicSyncGateway :
    ComicSingleSyncGateway,
    ComicLibraryScanGateway,
    ComicRebuildGateway

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
    suspend fun hideManga(comicId: Long): Either<LibrarySyncError, Unit>

    suspend fun deleteManga(comicId: Long): Either<LibrarySyncError, Unit>

    suspend fun updateMangaSettings(
        comicId: Long,
        externalSyncEnabled: Boolean,
    ): Either<LibrarySyncError, Unit>
}
