package br.acerola.comic.service.artwork

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import br.acerola.comic.error.message.IoError
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.relationship.BannerDao
import br.acerola.comic.local.entity.metadata.relationship.Banner
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.MediaFilePattern
import br.acerola.comic.service.file.FileStorageHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerSaver
    @Inject
    constructor(
        private val bannerDao: BannerDao,
        private val directoryDao: ComicDirectoryDao,
        private val fileStorageHandler: FileStorageHandler,
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun processBanner(
            rootUri: Uri,
            folderId: Long,
            bytes: ByteArray,
            bannerUrl: String,
            comicFolderName: String,
            comicRemoteInfoFk: Long,
        ): Either<IoError, Long> =
            withContext(Dispatchers.IO) {
                try {
                    val directory =
                        directoryDao.getDirectoryById(comicId = folderId)
                            ?: return@withContext IoError.FileNotFound("Directory not found in database").left()

                    val comicDir = DocumentFile.fromTreeUri(context, directory.path.toUri())

                    if (comicDir == null || !comicDir.isDirectory) {
                        AcerolaLogger.e(TAG, "Comic directory not accessible for ${directory.path}", LogSource.REPOSITORY)
                        return@withContext IoError.FileNotFound("Comic directory not accessible").left()
                    }

                    AcerolaLogger.d(TAG, "Saving banner to directory: ${comicDir.uri}", LogSource.REPOSITORY)

                    comicDir.listFiles().forEach { file ->
                        val fileName = file.name ?: return@forEach
                        if (MediaFilePattern.isBanner(fileName)) {
                            file.delete()
                        }
                    }

                    val fileName = MediaFilePattern.BANNER.defaultFileName

                    fileStorageHandler
                        .saveFile(
                            folder = comicDir,
                            fileName = fileName,
                            mimeType = "image/jpeg",
                            bytes = bytes,
                        ).flatMap {
                            val savedFile = comicDir.findFile(fileName)
                            val savedUriString = savedFile?.uri?.toString()

                            if (savedUriString != null) {
                                directoryDao.update(directory.copy(banner = savedUriString))
                            }

                            val bannerEntity =
                                Banner(
                                    fileName = fileName,
                                    url = bannerUrl,
                                    comicRemoteInfoFk = comicRemoteInfoFk,
                                )

                            val insertedId = bannerDao.insert(entity = bannerEntity)
                            val finalId =
                                if (insertedId != -1L) {
                                    insertedId
                                } else {
                                    val existing =
                                        bannerDao.getByFileNameAndMetadataId(
                                            fileName = fileName,
                                            comicRemoteInfoFk = comicRemoteInfoFk,
                                        ) ?: return@flatMap IoError.FileWriteError(fileName, Exception("Database inconsistency")).left()

                                    bannerDao.update(existing.copy(url = bannerUrl, fileName = fileName))
                                    existing.id
                                }
                            finalId.right()
                        }
                } catch (exception: Exception) {
                    AcerolaLogger.e(
                        TAG,
                        "Critical error processing banner for $comicFolderName",
                        LogSource.REPOSITORY,
                        exception,
                    )
                    IoError.FileWriteError(comicFolderName, exception).left()
                }
            }

        companion object {
            private const val TAG = "MangaSaveBannerService"
        }
    }
