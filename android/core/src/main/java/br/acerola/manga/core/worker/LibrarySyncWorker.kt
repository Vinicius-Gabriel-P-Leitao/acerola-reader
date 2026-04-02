package br.acerola.manga.core.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.library.DirectoryEngine
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
    @param:DirectoryEngine private val repository: MangaGateway<MangaDirectoryDto>
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_BASE_URI = "base_uri"
        const val KEY_MANGA_ID = "manga_id"

        const val SYNC_TYPE_INCREMENTAL = "incremental"
        const val SYNC_TYPE_REFRESH = "refresh"
        const val SYNC_TYPE_REBUILD = "rebuild"
        const val SYNC_TYPE_SPECIFIC = "specific"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result = coroutineScope {
        val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_INCREMENTAL
        val baseUriString = inputData.getString(KEY_BASE_URI)
        val baseUri = baseUriString?.toUri()
        val mangaId = inputData.getLong(KEY_MANGA_ID, -1L)

        val title = when (syncType) {
            SYNC_TYPE_INCREMENTAL -> context.getString(R.string.sync_library_title_incremental)
            SYNC_TYPE_REFRESH -> context.getString(R.string.sync_library_title_refresh)
            SYNC_TYPE_REBUILD -> context.getString(R.string.sync_library_title_rebuild)
            SYNC_TYPE_SPECIFIC -> context.getString(R.string.sync_library_title_generic)
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
            val resultEither = when (syncType) {
                SYNC_TYPE_INCREMENTAL -> repository.incrementalScan(baseUri)
                SYNC_TYPE_REFRESH -> repository.refreshLibrary(baseUri)
                SYNC_TYPE_REBUILD -> repository.rebuildLibrary(baseUri)
                SYNC_TYPE_SPECIFIC -> if (mangaId != -1L) repository.refreshManga(mangaId, baseUri) else {
                    progressJob.cancel()
                    return@coroutineScope Result.failure(workDataOf("error" to "Manga ID not found"))
                }
                else -> repository.incrementalScan(baseUri)
            }

            progressJob.cancel()

            resultEither.fold(
                ifLeft = {
                    val errorMsg = it.uiMessage.asString(context)
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_library_error_title),
                        errorMsg
                    )
                    Result.failure(workDataOf("error" to errorMsg))
                },
                ifRight = {
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_library_success_title),
                        context.getString(R.string.sync_library_success_message)
                    )
                    Result.success()
                }
            )
        } catch (exception: Exception) {
            progressJob.cancel()
            val errorMsg = exception.message ?: context.getString(R.string.sync_library_unexpected_error)
            notificationHelper.showFinishedNotification(
                context.getString(R.string.sync_library_fatal_error_title),
                errorMsg
            )
            Result.failure(workDataOf("error" to errorMsg))
        }
    }
}
