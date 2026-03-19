package br.acerola.manga.core.usecase.download

import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.service.compact.ArchiveCompactService
import br.acerola.manga.service.download.ChapterDownloadService
import br.acerola.manga.adapter.di.MangadexSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadChaptersUseCase @Inject constructor(
    @param:MangadexSource private val chapterDownloadService: ChapterDownloadService,
    private val archiveCompactService: ArchiveCompactService,
) {
    data class ChapterEntry(val id: String, val fileName: String)

    data class Result(val downloadedCount: Int, val errorCount: Int)

    suspend operator fun invoke(
        mangaFolder: DocumentFile,
        chapters: List<ChapterEntry>,
        coverUrl: String?,
        coverFileName: String?,
        onProgress: suspend (Int) -> Unit
    ): Result {
        if (!coverUrl.isNullOrBlank() && !coverFileName.isNullOrBlank()) {
            downloadCover(coverUrl, coverFileName, mangaFolder)
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

            onProgress(((downloadedCount.toFloat() / chapters.size) * 100).toInt())
        }

        return Result(downloadedCount = downloadedCount, errorCount = errorCount)
    }

    private suspend fun downloadPageEntries(pageUrls: List<String>): List<Pair<String, ByteArray>>? {
        val entries = mutableListOf<Pair<String, ByteArray>>()

        pageUrls.forEachIndexed { pageIndex, url ->
            val bytes = chapterDownloadService.downloadBytes(url) ?: return null

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
        val bytes = chapterDownloadService.downloadBytes(coverUrl) ?: return

        val extension = coverFileName.substringAfterLast('.', "jpg")
        val mimeType = if (extension == "png") "image/png" else "image/jpeg"

        archiveCompactService.saveImage(mangaFolder, coverFileName, mimeType, bytes)
    }
}
