package br.acerola.comic.adapter.contract.gateway

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface para observação do estado de sincronização de capítulos.
 */
interface ChapterSyncStatusGateway {
    val progress: StateFlow<Int>
    val isIndexing: StateFlow<Boolean>
}

/**
 * Interface para sincronização de capítulos.
 * Todas as Engines de capítulos implementam isso.
 */
interface ChapterSyncGateway : ChapterSyncStatusGateway {
    suspend fun refreshComicChapters(
        comicId: Long,
        baseUri: Uri? = null,
    ): Either<LibrarySyncError, Unit>
}

/**
 * Interface apenas para leitura de capítulos.
 * Implementada apenas pelas Engines que possuem persistência ou acesso a dados locais/remotos (Archive, Mangadex).
 */
interface ChapterReadGateway<T> {
    fun observeChapters(
        comicId: Long,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<T>

    suspend fun getChapterPage(
        comicId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): T
}

