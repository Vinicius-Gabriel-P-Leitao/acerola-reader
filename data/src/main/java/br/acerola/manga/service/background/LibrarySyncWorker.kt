package br.acerola.manga.service.background

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.repository.di.DirectoryFsOps
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.util.NotificationHelper
import br.acerola.manga.data.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    @param:DirectoryFsOps private val repository: MangaManagementRepository<MangaDirectoryDto>
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_BASE_URI = "base_uri"

        const val SYNC_TYPE_INCREMENTAL = "incremental"
        const val SYNC_TYPE_REFRESH = "refresh"
        const val SYNC_TYPE_REBUILD = "rebuild"
    }

    override suspend fun doWork(): Result = coroutineScope {
        val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_INCREMENTAL
        val baseUriString = inputData.getString(KEY_BASE_URI)
        val baseUri = baseUriString?.toUri()

        val title = when (syncType) {
            SYNC_TYPE_INCREMENTAL -> context.getString(R.string.sync_library_title_incremental)
            SYNC_TYPE_REFRESH -> context.getString(R.string.sync_library_title_refresh)
            SYNC_TYPE_REBUILD -> context.getString(R.string.sync_library_title_rebuild)
            else -> context.getString(R.string.sync_library_title_generic)
        }

        val builder = notificationHelper.createBaseNotification(title, context.getString(R.string.sync_library_description_scanning))
        setForeground(
            ForegroundInfo(
                NotificationHelper.SYNC_NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )

        val progressJob = launch {
            repository.progress.collectLatest { progress ->
                notificationHelper.updateProgress(builder, progress)
                setProgress(workDataOf("progress" to progress))
            }
        }

        try {
            val result = when (syncType) {
                SYNC_TYPE_INCREMENTAL -> repository.incrementalScan(baseUri)
                SYNC_TYPE_REFRESH -> repository.refreshLibrary(baseUri)
                SYNC_TYPE_REBUILD -> repository.rebuildLibrary(baseUri)
                else -> repository.incrementalScan(baseUri)
            }

            progressJob.cancel()

            result.fold(
                ifLeft = {
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_library_error_title),
                        it.uiMessage.asString(context)
                    )
                    Result.failure()
                },
                ifRight = {
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_library_success_title),
                        context.getString(R.string.sync_library_success_message)
                    )
                    Result.success()
                }
            )
        } catch (e: Exception) {
            progressJob.cancel()
            notificationHelper.showFinishedNotification(
                context.getString(R.string.sync_library_fatal_error_title),
                e.message ?: context.getString(R.string.sync_library_unexpected_error)
            )
            Result.failure()
        }
    }
}
