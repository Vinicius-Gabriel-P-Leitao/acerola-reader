package br.acerola.comic.usecase.sync

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.worker.contract.SyncType
import javax.inject.Inject

class SyncLibraryUseCase
    @Inject
    constructor(
        @DirectoryEngine private val singleSync: ComicSingleSyncGateway,
        @DirectoryEngine private val scanGateway: ComicLibraryScanGateway,
        @DirectoryEngine private val rebuildGateway: ComicRebuildGateway,
    ) {
        suspend fun execute(
            type: SyncType,
            comicId: Long,
            baseUri: Uri?,
        ): Either<LibrarySyncError, Unit> {
            return when (type) {
                SyncType.INCREMENTAL -> scanGateway.incrementalScan(baseUri)
                SyncType.REFRESH -> scanGateway.refreshLibrary(baseUri)
                SyncType.REBUILD -> rebuildGateway.rebuildLibrary(baseUri)
                SyncType.SPECIFIC -> {
                    if (comicId != -1L) {
                        singleSync.refreshManga(comicId, baseUri)
                    } else {
                        Either.Left(LibrarySyncError.UnexpectedError(Exception("Comic ID not found")))
                    }
                }
                else -> scanGateway.incrementalScan(baseUri)
            }
        }
    }
