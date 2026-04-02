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
            val directory = directoryDao.getMangaDirectoryById(mangaId = folderId)
                ?: return@withContext IoError.FileNotFound("Directory not found in database").left()

            val mangaDir = DocumentFile.fromTreeUri(context, Uri.parse(directory.path))

            if (mangaDir == null || !mangaDir.isDirectory) {
                AcerolaLogger.e(TAG, "Manga directory not accessible for ${directory.path}", LogSource.REPOSITORY)
                return@withContext IoError.FileNotFound("Manga directory not accessible").left()
            }
            
            AcerolaLogger.d(TAG, "Saving cover to directory: ${mangaDir.uri}", LogSource.REPOSITORY)
            
            // Delete existing covers
            mangaDir.listFiles().forEach { file ->
                val fileName = file.name ?: return@forEach
                if (MediaFilePattern.isCover(fileName)) {
                    file.delete()
                }
            }
 
            val fileName = MediaFilePattern.COVER.defaultFileName
            
            fileStorageHandler.saveFile(
                folder = mangaDir,
                fileName = fileName,
                mimeType = "image/jpeg",
                bytes = bytes
            ).flatMap {
                val savedFile = mangaDir.findFile(fileName)
                val savedUriString = savedFile?.uri?.toString()
                
                if (savedUriString != null) {
                    directoryDao.update(directory.copy(cover = savedUriString, lastModified = System.currentTimeMillis()))
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
                    ) ?: return@flatMap IoError.FileWriteError(fileName, Exception("Database inconsistency")).left()

                    coverDao.update(existing.copy(url = coverUrl, fileName = fileName))
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
