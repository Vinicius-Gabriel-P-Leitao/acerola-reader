package br.acerola.manga.core.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.acerola.manga.core.usecase.download.DownloadChaptersUseCase
import br.acerola.manga.data.R
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    private val workManager: WorkManager,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadChaptersUseCase: DownloadChaptersUseCase,
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val KEY_CHAPTER_IDS = "chapter_ids"
        const val KEY_CHAPTER_NUMBERS = "chapter_numbers"
        const val KEY_MANGA_TITLE = "manga_title"
        const val KEY_FILE_EXTENSION = "file_extension"
        const val KEY_BASE_URI = "base_uri"
        const val KEY_COVER_URL = "cover_url"
        const val KEY_COVER_FILE_NAME = "cover_file_name"
        const val DOWNLOAD_TAG = "chapter_download"

        const val KEY_TOTAL_CHAPTERS = "totalChapters"
        const val KEY_CURRENT_CHAPTER_ID = "currentChapterId"
        const val KEY_CURRENT_CHAPTER_FILE_NAME = "currentChapterFileName"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        return try {
            val chapterIds = inputData.getStringArray(KEY_CHAPTER_IDS) ?: return Result.failure(
                workDataOf("error" to context.getString(R.string.download_chapter_error_no_ids))
            )

            val chapterNumbers = inputData.getStringArray(KEY_CHAPTER_NUMBERS) ?: Array(chapterIds.size) { "" }
            val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: return Result.failure(
                workDataOf("error" to context.getString(R.string.download_chapter_error_no_title))
            )

            val fileExtension = inputData.getString(KEY_FILE_EXTENSION) ?: ".cbz"
            val baseUriString = inputData.getString(KEY_BASE_URI) ?: return Result.failure(
                workDataOf("error" to context.getString(R.string.download_chapter_error_no_uri))
            )

            val coverUrl = inputData.getString(KEY_COVER_URL)
            val coverFileName = inputData.getString(KEY_COVER_FILE_NAME)

            val builder = notificationHelper.createBaseNotification(
                context.getString(R.string.download_chapter_title),
                context.getString(R.string.download_chapter_description, mangaTitle)
            )

            setForeground(
                ForegroundInfo(
                    NotificationHelper.DOWNLOAD_NOTIFICATION_ID,
                    builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            )

            val libraryRoot = DocumentFile.fromTreeUri(context, baseUriString.toUri())
                ?: return Result.failure(workDataOf("error" to context.getString(R.string.download_chapter_error_access_folder)))

            val mangaFolder = libraryRoot.findFile(mangaTitle)
                ?: libraryRoot.createDirectory(mangaTitle)
                ?: return Result.failure(workDataOf("error" to context.getString(R.string.download_chapter_error_create_folder, mangaTitle)))

            val chapters = chapterIds.mapIndexed { index, id ->
                val number = chapterNumbers.getOrNull(index)?.takeIf { it.isNotBlank() } ?: id
                DownloadChaptersUseCase.ChapterEntry(id = id, fileName = "$number$fileExtension")
            }

            val result = downloadChaptersUseCase(
                mangaFolder = mangaFolder,
                chapters = chapters,
                coverUrl = coverUrl,
                coverFileName = coverFileName,
                onProgress = { progress, currentChapter ->
                    setProgress(
                        workDataOf(
                            WorkerContract.KEY_PROGRESS to progress,
                            KEY_CURRENT_CHAPTER_ID to currentChapter?.id,
                            KEY_CURRENT_CHAPTER_FILE_NAME to currentChapter?.fileName,
                            KEY_TOTAL_CHAPTERS to chapters.size,
                            KEY_MANGA_TITLE to mangaTitle
                        )
                    )
                    notificationHelper.updateProgress(builder, progress, NotificationHelper.DOWNLOAD_NOTIFICATION_ID)
                }
            )

            triggerPostDownloadSync(baseUriString)

            val resultTitle = context.getString(R.string.download_chapter_success_title)
            val resultMessage = context.getString(
                R.string.download_chapter_success_message,
                result.downloadedCount,
                chapterIds.size
            )
            notificationHelper.showFinishedNotification(resultTitle, resultMessage, NotificationHelper.DOWNLOAD_NOTIFICATION_ID)

            if (result.errorCount > 0 && result.downloadedCount == 0) {
                Result.failure(workDataOf("error" to context.getString(R.string.download_chapter_error_all_failed, result.errorCount)))
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            AcerolaLogger.e("ChapterDownloadWorker", "Fatal download error", LogSource.WORKER, e)
            val errorMsg = e.message ?: context.getString(R.string.download_chapter_error_fatal)
            notificationHelper.showFinishedNotification(
                context.getString(R.string.download_chapter_error_title),
                errorMsg,
                NotificationHelper.DOWNLOAD_NOTIFICATION_ID
            )
            Result.failure(workDataOf("error" to errorMsg))
        }
    }

    private fun triggerPostDownloadSync(baseUri: String) {
        val librarySyncRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setInputData(
                workDataOf(
                    LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_INCREMENTAL,
                    LibrarySyncWorker.KEY_BASE_URI to baseUri
                )
            )
            .addTag("library_sync")
            .build()

        val metadataSyncRequest = OneTimeWorkRequestBuilder<MetadataSyncWorker>()
            .setInputData(
                workDataOf(
                    MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                    MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_SYNC
                )
            )
            .addTag("metadata_sync")
            .build()

        workManager.beginWith(librarySyncRequest)
            .then(metadataSyncRequest)
            .enqueue()
    }
}
