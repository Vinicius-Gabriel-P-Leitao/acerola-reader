package br.acerola.manga.service.background

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import br.acerola.manga.data.R
import br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase
import br.acerola.manga.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope

@HiltWorker
class MetadataSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncMangaMetadataUseCase: SyncMangaMetadataUseCase
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val KEY_SYNC_SOURCE = "sync_source"
        const val KEY_DIRECTORY_ID = "directory_id"

        const val SOURCE_MANGADEX = "mangadex"
        const val SOURCE_COMICINFO = "comicinfo"
    }

    override suspend fun doWork(): Result = coroutineScope {
        val source = inputData.getString(KEY_SYNC_SOURCE) ?: SOURCE_MANGADEX
        val directoryId = inputData.getLong(KEY_DIRECTORY_ID, -1L)

        if (directoryId == -1L) return@coroutineScope Result.failure()

        val title = when (source) {
            SOURCE_MANGADEX -> context.getString(R.string.sync_metadata_title_mangadex)
            SOURCE_COMICINFO -> context.getString(R.string.sync_metadata_title_comicinfo)
            else -> context.getString(R.string.sync_metadata_title_generic)
        }

        val builder = notificationHelper.createBaseNotification(title, context.getString(R.string.sync_metadata_description_fetching))
        setForeground(
            ForegroundInfo(
                NotificationHelper.SYNC_NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )

        // Note: SyncMangaMetadataUseCase doesn't seem to have a progress flow directly yet,
        // but we can at least show it's running.

        try {
            val result = when (source) {
                SOURCE_MANGADEX -> syncMangaMetadataUseCase.syncFromMangadex(directoryId)
                SOURCE_COMICINFO -> syncMangaMetadataUseCase.syncFromComicInfo(directoryId)
                else -> syncMangaMetadataUseCase.syncFromMangadex(directoryId)
            }

            result.fold(
                ifLeft = {
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_metadata_error_title),
                        it.uiMessage.asString(context)
                    )
                    Result.failure()
                },
                ifRight = {
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_metadata_success_title),
                        context.getString(R.string.sync_metadata_success_message)
                    )
                    Result.success()
                }
            )
        } catch (e: Exception) {
            notificationHelper.showFinishedNotification(
                context.getString(R.string.sync_library_fatal_error_title),
                e.message ?: context.getString(R.string.sync_metadata_fetching_error)
            )
            Result.failure()
        }
    }
}
