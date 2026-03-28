package br.acerola.manga.service.artwork

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import br.acerola.manga.error.message.IoError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.relationship.CoverDao
import br.acerola.manga.local.entity.metadata.relationship.Cover
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.pattern.MediaFilePattern
import br.acerola.manga.service.file.FileStorageHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverSaver @Inject constructor(
    private val coverDao: CoverDao,
    private val directoryDao: MangaDirectoryDao,
    private val fileStorageHandler: FileStorageHandler,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun processCover(
        rootUri: Uri,
        folderId: Long,
        bytes: ByteArray,
        coverUrl: String,
        mangaFolderName: String,
        mangaRemoteInfoFk: Long
    ): Either<IoError, Long> = withContext(Dispatchers.IO) {
        try {
            val rootDir = DocumentFile.fromTreeUri(context, rootUri)
                ?: return@withContext IoError.FileNotFound("Root directory not accessible").left()

            val mangaDir = rootDir.findFile(mangaFolderName) 
                ?: rootDir.createDirectory(mangaFolderName)
                ?: return@withContext IoError.FileWriteError(mangaFolderName, Exception("Could not create manga directory")).left()

            val fileName = MediaFilePattern.COVER.defaultFileName
            
            fileStorageHandler.saveFile(
                folder = mangaDir,
                fileName = fileName,
                mimeType = "image/png",
                bytes = bytes
            ).flatMap {
                val savedUriString = mangaDir.findFile(fileName)?.uri?.toString()
                
                if (savedUriString != null) {
                    val directory = directoryDao.getMangaDirectoryById(mangaId = folderId)
                    if (directory != null) {
                        directoryDao.update(entity = directory.copy(cover = savedUriString, lastModified = System.currentTimeMillis()))
                    }
                }

                val coverEntity = Cover(
                    url = coverUrl,
                    mangaRemoteInfoFk = mangaRemoteInfoFk,
                    fileName = fileName,
                )

                val insertedId = coverDao.insert(entity = coverEntity)
                val finalId = if (insertedId != -1L) {
                    insertedId
                } else {
                    val existing = coverDao.getCoverByFileNameAndFk(
                        fileName = fileName,
                        mangaRemoteInfoFk = mangaRemoteInfoFk
                    ) ?: return@flatMap IoError.FileWriteError(fileName, Exception("Database inconsistency: Cover not found for update")).left()

                    coverDao.update(entity = existing.copy(url = coverUrl, fileName = fileName))
                    existing.id
                }
                finalId.right()
            }
        } catch (exception: Exception) {
            AcerolaLogger.e(TAG, "Critical error processing cover for $mangaFolderName", LogSource.REPOSITORY, exception)
            IoError.FileWriteError(mangaFolderName, exception).left()
        }
    }

    companion object {
        private const val TAG = "MangaSaveCoverService"
    }
}
