package br.acerola.comic.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.acerola.comic.usecase.AnilistCase
import br.acerola.comic.usecase.ComicInfoCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.library.SyncLibraryUseCase
import br.acerola.comic.usecase.metadata.SyncComicMetadataUseCase
import br.acerola.comic.data.R
import br.acerola.comic.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltWorker
class MetadataSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncComicMetadataUseCase: SyncComicMetadataUseCase,
    @param:AnilistCase private val anilistSyncUseCase: SyncLibraryUseCase,
    @param:MangadexCase private val mangadexSyncUseCase: SyncLibraryUseCase,
    @param:ComicInfoCase private val comicInfoSyncUseCase: SyncLibraryUseCase,
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val KEY_SYNC_SOURCE = "sync_source"
        const val KEY_DIRECTORY_ID = "directory_id"
        const val KEY_SYNC_TYPE = "sync_type"

        const val SOURCE_MANGADEX = "mangadex"
        const val SOURCE_COMICINFO = "comicinfo"
        const val SOURCE_ANILIST = "anilist"

        const val SYNC_TYPE_SYNC = "sync"
        const val SYNC_TYPE_RESCAN = "rescan"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result = coroutineScope {
        val source = inputData.getString(KEY_SYNC_SOURCE) ?: SOURCE_MANGADEX
        val directoryId = inputData.getLong(KEY_DIRECTORY_ID, -1L)
        val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_SYNC

        val title = when (source) {
            SOURCE_MANGADEX -> context.getString(R.string.sync_metadata_title_mangadex)
            SOURCE_COMICINFO -> context.getString(R.string.sync_metadata_title_comicinfo)
            SOURCE_ANILIST -> context.getString(R.string.sync_metadata_title_anilist)
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

        val progressJob = launch {
            val progressFlow = when (source) {
                SOURCE_MANGADEX -> mangadexSyncUseCase.progress
                SOURCE_COMICINFO -> comicInfoSyncUseCase.progress
                SOURCE_ANILIST -> anilistSyncUseCase.progress
                else -> mangadexSyncUseCase.progress
            }

            progressFlow.collectLatest { progress ->
                notificationHelper.updateProgress(builder, progress)
                setProgress(workDataOf("progress" to progress))
            }
        }

        try {
            val result = if (directoryId != -1L) {
                // Single comic sync
                when (source) {
                    SOURCE_MANGADEX -> syncComicMetadataUseCase.syncFromMangadex(directoryId)
                    SOURCE_COMICINFO -> syncComicMetadataUseCase.syncFromComicInfo(directoryId)
                    SOURCE_ANILIST -> syncComicMetadataUseCase.syncFromAnilist(directoryId)
                    else -> syncComicMetadataUseCase.syncFromMangadex(directoryId)
                }
            } else {
                // Library-wide sync
                val useCase = when (source) {
                    SOURCE_MANGADEX -> mangadexSyncUseCase
                    SOURCE_COMICINFO -> comicInfoSyncUseCase
                    SOURCE_ANILIST -> anilistSyncUseCase
                    else -> mangadexSyncUseCase
                }

                when (syncType) {
                    SYNC_TYPE_SYNC -> useCase.sync(baseUri = null)
                    SYNC_TYPE_RESCAN -> useCase.rescan(baseUri = null)
                    else -> useCase.sync(baseUri = null)
                }
            }

            progressJob.cancel()

            result.fold(
                ifLeft = {
                    val errorMsg = it.uiMessage.asString(context)
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_metadata_error_title),
                        errorMsg
                    )
                    Result.failure(workDataOf("error" to errorMsg))
                },
                ifRight = {
                    notificationHelper.showFinishedNotification(
                        context.getString(R.string.sync_metadata_success_title),
                        context.getString(R.string.sync_metadata_success_message)
                    )
                    Result.success()
                }
            )
        } catch (exception: Exception) {
            progressJob.cancel()
            val errorMsg = exception.message ?: context.getString(R.string.sync_metadata_fetching_error)
            notificationHelper.showFinishedNotification(
                context.getString(R.string.sync_library_fatal_error_title),
                errorMsg
            )
            Result.failure(workDataOf("error" to errorMsg))
        }
    }
}
