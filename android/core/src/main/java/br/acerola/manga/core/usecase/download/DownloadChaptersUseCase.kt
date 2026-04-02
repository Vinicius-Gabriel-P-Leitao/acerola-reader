package br.acerola.manga.core.usecase.download

import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.adapter.metadata.mangadex.MangadexSource
import br.acerola.manga.service.compact.CbzCompressor
import br.acerola.manga.service.download.DownloadManager
import br.acerola.manga.service.file.FileStorageHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadChaptersUseCase @Inject constructor(
    @param:MangadexSource private val downloadManager: DownloadManager,
    private val archiveCompactService: CbzCompressor,
    private val fileStorageHandler: FileStorageHandler,
) {
    data class ChapterEntry(val id: String, val fileName: String)

    data class Result(val downloadedCount: Int, val errorCount: Int)

    suspend operator fun invoke(
        mangaFolder: DocumentFile,
        chapters: List<ChapterEntry>,
        coverUrl: String?,
        coverFileName: String?,
        onProgress: suspend (progress: Int, currentChapter: ChapterEntry?) -> Unit
    ): Result {
        if (!coverUrl.isNullOrBlank() && !coverFileName.isNullOrBlank()) {
            downloadCover(coverUrl, coverFileName, mangaFolder)
        }

        var downloadedCount = 0
        var errorCount = 0

        chapters.forEachIndexed { _, entry ->
            onProgress(((downloadedCount.toFloat() / chapters.size) * 100).toInt(), entry)

            if (mangaFolder.findFile(entry.fileName) != null) {
                downloadedCount++
                onProgress(((downloadedCount.toFloat() / chapters.size) * 100).toInt(), entry)
                return@forEachIndexed
            }

            val pageUrls = downloadManager.getPageUrls(entry.id).getOrNull()

            if (pageUrls.isNullOrEmpty()) {
                errorCount++
                return@forEachIndexed
            }

            val pageEntries = downloadPageEntries(pageUrls)

            if (pageEntries == null || pageEntries.size < pageUrls.size) {
                errorCount++
                return@forEachIndexed
            }

            archiveCompactService.createCbz(mangaFolder, entry.fileName, pageEntries)
                .fold(ifLeft = { errorCount++ }, ifRight = { downloadedCount++ })

            onProgress(((downloadedCount.toFloat() / chapters.size) * 100).toInt(), entry)
        }

        onProgress(100, null)
        return Result(downloadedCount = downloadedCount, errorCount = errorCount)
    }

    private suspend fun downloadPageEntries(pageUrls: List<String>): List<Pair<String, ByteArray>>? {
        val entries = mutableListOf<Pair<String, ByteArray>>()

        pageUrls.forEachIndexed { pageIndex, url ->
            val bytes = downloadManager.downloadBytes(url) ?: return null

            val fileName = url.substringAfterLast('/').substringBefore('?')
            val extension = fileName.substringAfterLast('.', "jpg")

            entries.add("%04d.$extension".format(pageIndex) to bytes)
        }
        return entries
    }

    private suspend fun downloadCover(
        coverUrl: String,
        coverFileName: String,
        mangaFolder: DocumentFile
    ) {
        if (mangaFolder.findFile(coverFileName) != null) return
        val bytes = downloadManager.downloadBytes(coverUrl) ?: return

        val extension = coverFileName.substringAfterLast('.', "jpg")
        val mimeType = if (extension == "png") "image/png" else "image/jpeg"

        fileStorageHandler.saveFile(mangaFolder, coverFileName, mimeType, bytes)
    }
}
