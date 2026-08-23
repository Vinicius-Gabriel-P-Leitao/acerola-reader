package br.acerola.comic.service.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import br.acerola.comic.error.message.IoError
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.media.MediaFile
import br.acerola.comic.service.file.FileStorageHandler
import br.acerola.comic.service.reader.ChapterSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverExtractor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val directoryDao: ComicDirectoryDao,
        private val chapterArchiveDao: ChapterArchiveDao,
        private val volumeArchiveDao: VolumeArchiveDao,
        private val fileStorageHandler: FileStorageHandler,
        private val chapterSourceFactory: ChapterSourceFactory,
    ) {
        suspend fun extractFirstPageAsCover(comicId: Long): Either<IoError, Unit> =
            withContext(Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Extracting comic cover for comic: $comicId", LogSource.SERVICE)
                val directory =
                    directoryDao.getDirectoryById(comicId)
                        ?: return@withContext IoError.FileNotFound("Comic directory not found in DB").left()

                // Pega o primeiro capítulo da obra toda (Dao já ordena numericamente Vol ASC -> Ch ASC)
                val chapters = chapterArchiveDao.getChaptersByDirectoryId(comicId).first()
                val firstChapterJoin = chapters.firstOrNull()
                val firstChapter =
                    firstChapterJoin?.chapter
                        ?: return@withContext IoError.FileNotFound("No chapters found for this comic").left()

                AcerolaLogger.i(
                    TAG,
                    "Selected chapter for COMIC cover: ${firstChapter.chapter} (Volume: ${firstChapterJoin.volume?.name ?: "Root"})",
                    LogSource.SERVICE,
                )

                val rootUri = directory.path.toUri()
                val rootDoc =
                    DocumentFile.fromTreeUri(context, rootUri) ?: DocumentFile.fromSingleUri(context, rootUri)
                        ?: return@withContext IoError.FileReadError(directory.path, Exception("Could not resolve root folder")).left()

                extractAndSaveCover(
                    chapter = firstChapter,
                    folderDoc = rootDoc, // Garante que salva na raiz da comic
                    onSuccess = { uri ->
                        AcerolaLogger.i(TAG, "Comic cover saved to root: $uri", LogSource.SERVICE)
                        directoryDao.update(
                            directory.copy(
                                lastModified = System.currentTimeMillis(),
                                cover = uri,
                            ),
                        )
                    },
                )
            }

        suspend fun extractVolumeCover(
            comicId: Long,
            volumeId: Long,
        ): Either<IoError, Unit> =
            withContext(Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Extracting volume cover for vol: $volumeId", LogSource.SERVICE)
                val volume =
                    volumeArchiveDao.getVolumeById(volumeId)
                        ?: return@withContext IoError.FileNotFound("Volume not found in DB").left()

                // Pega o primeiro capítulo DESTE volume (ordenado numericamente)
                val chapters =
                    chapterArchiveDao.getChaptersByVolumePaged(
                        comicId = comicId,
                        volumeId = volumeId,
                        pageSize = 1,
                        offset = 0,
                    )

                val firstChapter =
                    chapters.firstOrNull()?.chapter
                        ?: return@withContext IoError.FileNotFound("No chapters found for this volume").left()

                val volumeUri = volume.path.toUri()
                val volumeDoc =
                    DocumentFile.fromTreeUri(context, volumeUri) ?: DocumentFile.fromSingleUri(context, volumeUri)
                        ?: return@withContext IoError.FileReadError(volume.path, Exception("Could not resolve volume folder")).left()

                extractAndSaveCover(
                    chapter = firstChapter,
                    folderDoc = volumeDoc, // Garante que salva dentro da pasta do volume
                    onSuccess = { uri ->
                        AcerolaLogger.i(TAG, "Volume cover saved to volume folder: $uri", LogSource.SERVICE)
                        volumeArchiveDao.update(
                            volume.copy(
                                lastModified = System.currentTimeMillis(),
                                cover = uri,
                            ),
                        )
                    },
                )
            }

        private suspend fun extractAndSaveCover(
            chapter: ChapterArchive,
            folderDoc: DocumentFile,
            onSuccess: suspend (String) -> Unit,
        ): Either<IoError, Unit> {
            val chapterDto = chapter.toViewDto()
            AcerolaLogger.d(TAG, "Extracting from: ${chapter.chapter}", LogSource.SERVICE)

            return chapterSourceFactory
                .create(chapterDto)
                .mapLeft {
                    IoError.FileReadError(chapterDto.path, Exception(it.toString()))
                }.flatMap { source ->
                    try {
                        source
                            .openPage(0)
                            .mapLeft {
                                IoError.FileReadError(chapterDto.path, Exception(it.toString()))
                            }.flatMap { inputStream ->
                                val bitmap =
                                    BitmapFactory.decodeStream(inputStream)
                                        ?: return@flatMap IoError
                                            .FileReadError(
                                                chapterDto.path,
                                                Exception("Failed to decode bitmap"),
                                            ).left()

                                val outputStream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

                                val bytes = outputStream.toByteArray()
                                bitmap.recycle()

                                // Limpa apenas capas existentes na pasta ALVO (root ou volume)
                                folderDoc.listFiles().forEach { file ->
                                    val name = file.name ?: return@forEach
                                    if (MediaFile.isCover(name)) file.delete()
                                }

                                fileStorageHandler
                                    .saveFile(
                                        folder = folderDoc,
                                        fileName = MediaFile.COVER.defaultFileName,
                                        mimeType = "image/jpeg",
                                        bytes = bytes,
                                    ).also { result ->
                                        result.onRight { uri -> onSuccess(uri) }
                                    }.map { Unit }
                            }
                    } finally {
                        source.close()
                    }
                }
        }

        companion object {
            private const val TAG = "CoverExtractor"
        }
    }
