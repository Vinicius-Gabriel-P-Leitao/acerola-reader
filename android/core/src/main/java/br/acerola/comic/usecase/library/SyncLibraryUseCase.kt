package br.acerola.comic.usecase.library

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.error.message.LibrarySyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * UseCase responsável pela sincronização e reindexação da biblioteca de quadrinhos.
 * Pode ser injetado com qualificadores para operar em diferentes fontes (Local/Remoto).
 */
class SyncLibraryUseCase(
    private val scanGateway: ComicLibraryScanGateway? = null,
    private val rebuildGateway: ComicRebuildGateway? = null,
    private val chapterGateway: ChapterSyncStatusGateway? = null,
) {
    private val mangaIndexing: Flow<Boolean> =
        combine(
            scanGateway?.isIndexing ?: MutableStateFlow(false),
            rebuildGateway?.isIndexing ?: MutableStateFlow(false),
        ) { scanBusy, rebuildBusy ->
            scanBusy || rebuildBusy
        }

    private val mangaProgress: Flow<Int> =
        combine(
            scanGateway?.progress ?: MutableStateFlow(-1),
            rebuildGateway?.progress ?: MutableStateFlow(-1),
            scanGateway?.isIndexing ?: MutableStateFlow(false),
            rebuildGateway?.isIndexing ?: MutableStateFlow(false),
        ) { scanProg, rebuildProg, scanBusy, rebuildBusy ->
            when {
                scanBusy -> scanProg
                rebuildBusy -> rebuildProg
                else -> -1
            }
        }

    val progress: Flow<Int> =
        combine(
            mangaProgress,
            chapterGateway?.progress ?: MutableStateFlow(-1),
            mangaIndexing,
            chapterGateway?.isIndexing ?: MutableStateFlow(false),
        ) { mProg, cProg, mBusy, cBusy ->
            when {
                mBusy -> mProg
                cBusy -> cProg
                else -> -1
            }
        }

    val isIndexing: Flow<Boolean> =
        combine(
            mangaIndexing,
            chapterGateway?.isIndexing ?: MutableStateFlow(false),
        ) { mBusy, cBusy ->
            mBusy || cBusy
        }

    suspend fun sync(baseUri: Uri? = null): Either<LibrarySyncError, Unit> =
        scanGateway?.incrementalScan(baseUri) ?: Either.Left(LibrarySyncError.UnexpectedError(Exception("Sync not supported")))

    suspend fun rescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> =
        scanGateway?.refreshLibrary(baseUri) ?: Either.Left(LibrarySyncError.UnexpectedError(Exception("Rescan not supported")))

    suspend fun deepRescan(baseUri: Uri? = null): Either<LibrarySyncError, Unit> =
        rebuildGateway?.rebuildLibrary(baseUri) ?: Either.Left(LibrarySyncError.UnexpectedError(Exception("Deep rescan not supported")))
}
