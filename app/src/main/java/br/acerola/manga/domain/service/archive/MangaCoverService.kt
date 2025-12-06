package br.acerola.manga.domain.service.archive

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.cover.CoverDao
import br.acerola.manga.domain.model.metadata.cover.Cover
import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchCoverService
import br.acerola.manga.shared.dto.metadata.CoverDto

class MangaCoverService(
    private val context: Context,
    private val coverDao: CoverDao,
    private val folderDao: MangaFolderDao,
    private val downloadService: MangaDexFetchCoverService
) {
    suspend fun processCover(
        rootUri: Uri,
        folderId: Long,
        coverDto: CoverDto,
        mangaFolderName: String,
    ): Long {
        var savedUriString: String? = null

        try {
            val rootDir = DocumentFile.fromTreeUri(context, rootUri)

            if (rootDir != null && rootDir.exists()) {
                var mangaDir = rootDir.findFile(mangaFolderName)

                Log.d("MangaCoverService", mangaDir.toString())

                if (mangaDir == null) {
                    mangaDir = rootDir.createDirectory(mangaFolderName)
                }

                if (mangaDir != null && mangaDir.canWrite()) {
                    val bytes = downloadService.searchCover(coverDto.url)
                    Log.d("MangaCoverService", bytes.toString())
                    val finalFileName = "cover.png"

                    val oldFile = mangaDir.findFile(finalFileName)
                    if (oldFile != null && oldFile.exists()) {
                        oldFile.delete()
                    }

                    val newFile = mangaDir.createFile("image/png", finalFileName)

                    if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        savedUriString = newFile.uri.toString()
                    }
                }
            }
        } catch (exception: Exception) {
            // TODO: Tratar erros
            exception.printStackTrace()
        }

        if (savedUriString != null) {
            val folderEntity = folderDao.getMangaFolderById(mangaId = folderId)
            if (folderEntity != null) {
                folderDao.update(entity = folderEntity.copy(cover = savedUriString))
            }
        }

        val coverEntity = Cover(
            id = folderId,
            mirrorId = coverDto.id,
            fileName = "cover.png",
            url = coverDto.url,
        )

        return run {
            coverDao.update(coverEntity)
            coverEntity.id
        }
    }

    private fun fileExists(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.close()
            true
        } catch (exception: Exception) {
            false
        }
    }
}