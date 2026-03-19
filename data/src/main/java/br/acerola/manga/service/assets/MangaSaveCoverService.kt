package br.acerola.manga.service.assets

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.pattern.MediaFilePattern
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.relationship.CoverDao
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSaveCoverService @Inject constructor(
    private val coverDao: CoverDao,
    private val directoryDao: MangaDirectoryDao,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun processCover(
        rootUri: Uri,
        folderId: Long,
        bytes: ByteArray,
        coverUrl: String,
        mangaFolderName: String,
        mangaRemoteInfoFk: Long
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
                    val finalFileName = MediaFilePattern.COVER.defaultFileName

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

        // TODO: Fazer toModel
        val coverEntity = Cover(
            fileName = MediaFilePattern.COVER.defaultFileName,
            url = coverUrl,
            mangaRemoteInfoFk = mangaRemoteInfoFk
        )

        val insertedId = coverDao.insert(entity = coverEntity)

        return if (insertedId != -1L) {
            insertedId
        } else {
            val existing = coverDao.getCoverByFileNameAndFk(
                fileName = MediaFilePattern.COVER.defaultFileName,
                mangaRemoteInfoFk = mangaRemoteInfoFk
            ) ?: throw IllegalStateException("Cover not found for fk: $mangaRemoteInfoFk")

            coverDao.update(entity = existing.copy(url = coverUrl, fileName = MediaFilePattern.COVER.defaultFileName))
            existing.id
        }
    }

}
