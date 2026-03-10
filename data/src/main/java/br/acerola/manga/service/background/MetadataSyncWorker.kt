package br.acerola.manga.service.background

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase
import br.acerola.manga.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltWorker
class MetadataSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncMangaMetadataUseCase: SyncMangaMetadataUseCase
) : CoroutineWorker(appContext, workerParams) {

    private val notificationHelper = NotificationHelper(appContext)

    companion object {
        const val KEY_SYNC_SOURCE = "sync_source"
        const val KEY_DIRECTORY_ID = "directory_id"

        const val SOURCE_MANGADEX = "mangadex"
        const val SOURCE_COMICINFO = "comicinfo"
    }

    override suspend fun doWork(): Result {
        val source = inputData.getString(KEY_SYNC_SOURCE) ?: SOURCE_MANGADEX
        val directoryId = inputData.getLong(KEY_DIRECTORY_ID, -1L)

        if (directoryId == -1L) return Result.failure()

        val title = when (source) {
            SOURCE_MANGADEX -> "Buscando Metadados (MangaDex)"
            SOURCE_COMICINFO -> "Lendo ComicInfo.xml"
            else -> "Sincronizando Metadados..."
        }

        val builder = notificationHelper.createBaseNotification(title, "Buscando informações para sua biblioteca...")
        setForeground(
            ForegroundInfo(
                NotificationHelper.SYNC_NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )

        // Note: SyncMangaMetadataUseCase doesn't seem to have a progress flow directly yet,
        // but we can at least show it's running.

        return try {
            val result = when (source) {
                SOURCE_MANGADEX -> syncMangaMetadataUseCase.syncFromMangadex(directoryId)
                SOURCE_COMICINFO -> syncMangaMetadataUseCase.syncFromComicInfo(directoryId)
                else -> syncMangaMetadataUseCase.syncFromMangadex(directoryId)
            }

            result.fold(
                ifLeft = { 
                    notificationHelper.showFinishedNotification("Erro nos Metadados", it.uiMessage.asString(appContext))
                    Result.failure() 
                },
                ifRight = { 
                    notificationHelper.showFinishedNotification("Metadados Atualizados", "Informações baixadas com sucesso.")
                    Result.success() 
                }
            )
        } catch (e: Exception) {
            notificationHelper.showFinishedNotification("Erro Fatal", e.message ?: "Erro ao buscar metadados.")
            Result.failure()
        }
    }
}
