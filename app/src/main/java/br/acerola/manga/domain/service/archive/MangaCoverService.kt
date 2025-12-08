package br.acerola.manga.domain.service.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.data.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.data.dao.database.metadata.cover.CoverDao
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

                if (mangaDir == null) {
                    mangaDir = rootDir.createDirectory(mangaFolderName)
                }

                if (mangaDir != null && mangaDir.canWrite()) {
                    val bytes = downloadService.searchCover(coverDto.url)
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
            println(exception)
        }

        if (savedUriString != null) {
            val folderEntity = folderDao.getMangaFolderById(mangaId = folderId)
            if (folderEntity != null) {
                folderDao.update(entity = folderEntity.copy(cover = savedUriString))
            }
        }

        val insertedId = coverDao.insert(
            entity = Cover(
                mirrorId = coverDto.id,
                fileName = "cover.png",
                url = coverDto.url,
            )
        )

        return if (insertedId != -1L) {
            insertedId
        } else {
            val existing = coverDao.getCoverByMirrorId(mirrorId = coverDto.id)
                ?: throw IllegalStateException("O cover não foi encontrado.: ${coverDto.id}")

            coverDao.update(existing.copy(url = coverDto.url, fileName = "cover.png"))
            existing.id
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