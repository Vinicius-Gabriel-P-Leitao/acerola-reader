package br.acerola.manga.service.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.cover.CoverDao
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexFetchCoverService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSaveCoverService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val coverDao: CoverDao,
    private val directoryDao: MangaDirectoryDao,
    private val downloadCoverService: MangadexFetchCoverService
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
                    val bytes = downloadCoverService.searchCover(coverDto.url)
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
            val directory = directoryDao.getMangaDirectoryById(mangaId = folderId)
            if (directory != null) {
                directoryDao.update(entity = directory.copy(cover = savedUriString))
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

            coverDao.update(entity = existing.copy(url = coverDto.url, fileName = "cover.png"))
            existing.id
        }
    }

}