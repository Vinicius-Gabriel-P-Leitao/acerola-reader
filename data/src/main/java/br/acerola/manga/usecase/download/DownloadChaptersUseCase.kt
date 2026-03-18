package br.acerola.manga.usecase.download

import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.service.compact.ArchiveCompactService
import br.acerola.manga.service.download.ChapterDownloadService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadChaptersUseCase @Inject constructor(
    private val chapterDownloadService: ChapterDownloadService,
    private val archiveCompactService: ArchiveCompactService
) {
    data class ChapterEntry(val id: String, val fileName: String)

    data class Result(val downloadedCount: Int, val errorCount: Int)

    suspend operator fun invoke(
        mangaFolder: DocumentFile,
        chapters: List<ChapterEntry>,
        coverUrl: String?,
        onProgress: suspend (Int) -> Unit
    ): Result {
        if (!coverUrl.isNullOrBlank()) {
            downloadCover(coverUrl, mangaFolder)
        }

        var downloadedCount = 0
        var errorCount = 0

        chapters.forEachIndexed { _, entry ->
            if (mangaFolder.findFile(entry.fileName) != null) {
                downloadedCount++
                onProgress(((downloadedCount.toFloat() / chapters.size) * 100).toInt())
                return@forEachIndexed
            }

            val pageUrls = chapterDownloadService.getPageUrls(entry.id).getOrNull()
            if (pageUrls == null) {
                errorCount++
                return@forEachIndexed
            }

            val pageEntries = pageUrls.mapIndexedNotNull { pageIndex, url ->
                val bytes = chapterDownloadService.downloadBytes(url) ?: return@mapIndexedNotNull null
                val extension = url.substringAfterLast('.', "jpg")

                "%04d.$extension".format(pageIndex) to bytes
            }

            archiveCompactService.createCbz(mangaFolder, entry.fileName, pageEntries)
                .fold(ifLeft = { errorCount++ }, ifRight = { downloadedCount++ })

            onProgress(((downloadedCount.toFloat() / chapters.size) * 100).toInt())
        }

        return Result(downloadedCount = downloadedCount, errorCount = errorCount)
    }

    private suspend fun downloadCover(coverUrl: String, mangaFolder: DocumentFile) {
        val extension = coverUrl.substringAfterLast('.', "jpg").substringBefore('?')
        val coverFileName = "cover.$extension"
        if (mangaFolder.findFile(coverFileName) != null) return

        val bytes = chapterDownloadService.downloadBytes(coverUrl) ?: return
        val mimeType = if (extension == "png") "image/png" else "image/jpeg"
        archiveCompactService.saveImage(mangaFolder, coverFileName, mimeType, bytes)
    }
}
