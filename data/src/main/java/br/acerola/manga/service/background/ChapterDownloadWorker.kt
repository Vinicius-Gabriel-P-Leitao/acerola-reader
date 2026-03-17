package br.acerola.manga.service.background

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.acerola.manga.data.R
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chapterInfoApi: MangadexChapterInfoApi,
    private val downloadApi: MangadexDownloadApi,
    private val workManager: WorkManager,
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val KEY_CHAPTER_IDS = "chapter_ids"
        const val KEY_CHAPTER_NUMBERS = "chapter_numbers"
        const val KEY_MANGA_TITLE = "manga_title"
        const val KEY_FILE_EXTENSION = "file_extension"
        const val KEY_BASE_URI = "base_uri"
        const val DOWNLOAD_TAG = "chapter_download"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val chapterIds = inputData.getStringArray(KEY_CHAPTER_IDS) ?: return@withContext Result.failure(
            workDataOf("error" to "No chapter IDs provided")
        )
        val chapterNumbers = inputData.getStringArray(KEY_CHAPTER_NUMBERS) ?: Array(chapterIds.size) { "" }
        val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: return@withContext Result.failure(
            workDataOf("error" to "No manga title provided")
        )
        val fileExtension = inputData.getString(KEY_FILE_EXTENSION) ?: ".cbz"
        val baseUriString = inputData.getString(KEY_BASE_URI) ?: return@withContext Result.failure(
            workDataOf("error" to "No base URI provided")
        )

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

        val baseUri = baseUriString.toUri()
        val libraryRoot = DocumentFile.fromTreeUri(context, baseUri)
            ?: return@withContext Result.failure(workDataOf("error" to "Cannot access library folder"))

        val mangaFolder = libraryRoot.findFile(mangaTitle)
            ?: libraryRoot.createDirectory(mangaTitle)
            ?: return@withContext Result.failure(workDataOf("error" to "Cannot create manga folder: $mangaTitle"))

        var downloadedCount = 0
        var errorCount = 0

        chapterIds.forEachIndexed { index, chapterId ->
            val chapterNumber = chapterNumbers.getOrNull(index)?.takeIf { it.isNotBlank() } ?: chapterId
            val fileName = "$chapterNumber$fileExtension"

            val existingFile = mangaFolder.findFile(fileName)
            if (existingFile != null) {
                downloadedCount++
                val progress = ((downloadedCount.toFloat() / chapterIds.size) * 100).toInt()
                setProgress(workDataOf("progress" to progress))
                notificationHelper.updateProgress(builder, progress, NotificationHelper.DOWNLOAD_NOTIFICATION_ID)
                return@forEachIndexed
            }

            val sourceResult = safeApiCall(timeoutMs = 500L) { chapterInfoApi.getChapterImages(chapterId) }
            val source = sourceResult.getOrNull()
            if (source == null) {
                errorCount++
                return@forEachIndexed
            }

            val pageUrls = source.chapter.data.map { "${source.baseUrl}/data/${source.chapter.hash}/$it" }

            val cbzFile = mangaFolder.createFile("application/zip", fileName)
            if (cbzFile == null) {
                errorCount++
                return@forEachIndexed
            }

            try {
                context.contentResolver.openOutputStream(cbzFile.uri)?.use { outStream ->
                    ZipOutputStream(outStream).use { zip ->
                        pageUrls.forEachIndexed { pageIndex, url ->
                            val pageResult = safeApiCall(timeoutMs = 200L) { downloadApi.downloadFile(url) }
                            val bytes = pageResult.getOrNull()?.bytes()
                            if (bytes != null) {
                                val extension = url.substringAfterLast('.', "jpg")
                                zip.putNextEntry(ZipEntry("%04d.$extension".format(pageIndex)))
                                zip.write(bytes)
                                zip.closeEntry()
                            }
                        }
                    }
                }
                downloadedCount++
            } catch (e: Exception) {
                cbzFile.delete()
                errorCount++
            }

            val progress = ((downloadedCount.toFloat() / chapterIds.size) * 100).toInt()
            setProgress(workDataOf("progress" to progress))
            notificationHelper.updateProgress(builder, progress, NotificationHelper.DOWNLOAD_NOTIFICATION_ID)
        }

        triggerLibrarySync(baseUriString)

        val resultTitle = context.getString(R.string.download_chapter_success_title)
        val resultMessage = context.getString(
            R.string.download_chapter_success_message,
            downloadedCount,
            chapterIds.size
        )
        notificationHelper.showFinishedNotification(resultTitle, resultMessage, NotificationHelper.DOWNLOAD_NOTIFICATION_ID)

        if (errorCount > 0 && downloadedCount == 0) {
            Result.failure(workDataOf("error" to "All $errorCount downloads failed"))
        } else {
            Result.success()
        }
    }

    private fun triggerLibrarySync(baseUri: String) {
        val syncRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setInputData(
                workDataOf(
                    LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_INCREMENTAL,
                    LibrarySyncWorker.KEY_BASE_URI to baseUri
                )
            )
            .addTag("library_sync")
            .build()

        workManager.enqueueUniqueWork(
            "library_sync_after_download",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
