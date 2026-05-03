package br.acerola.comic.sync

import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.comic.worker.contract.WorkerContract
import br.acerola.comic.worker.sync.LibrarySyncWorker
import javax.inject.Inject

class WorkManagerLibrarySyncScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) : LibrarySyncScheduler {
        override fun enqueueIncremental(baseUri: Uri?) {
            enqueue(LibrarySyncWorker.SYNC_TYPE_INCREMENTAL, -1L, baseUri)
        }

        override fun enqueueRefresh(baseUri: Uri?) {
            enqueue(LibrarySyncWorker.SYNC_TYPE_REFRESH, -1L, baseUri)
        }

        override fun enqueueRebuild(baseUri: Uri?) {
            enqueue(LibrarySyncWorker.SYNC_TYPE_REBUILD, -1L, baseUri)
        }

        override fun enqueueSpecific(
            comicId: Long,
            baseUri: Uri?,
        ) {
            enqueue(LibrarySyncWorker.SYNC_TYPE_SPECIFIC, comicId, baseUri)
        }

        private fun enqueue(
            type: String,
            comicId: Long,
            baseUri: Uri?,
        ) {
            val syncRequest =
                OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                    .setInputData(
                        workDataOf(
                            LibrarySyncWorker.KEY_SYNC_TYPE to type,
                            LibrarySyncWorker.KEY_BASE_URI to baseUri?.toString(),
                            LibrarySyncWorker.KEY_MANGA_ID to comicId,
                        ),
                    ).addTag(WorkerContract.TAG_LIBRARY_SYNC)
                    .build()

            val uniqueName =
                if (comicId != -1L) {
                    "${WorkerContract.TAG_LIBRARY_SYNC}_$comicId"
                } else {
                    WorkerContract.TAG_LIBRARY_SYNC
                }

            workManager.enqueueUniqueWork(
                uniqueName,
                ExistingWorkPolicy.KEEP,
                syncRequest,
            )
        }
    }
