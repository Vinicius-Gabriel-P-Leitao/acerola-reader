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
                AcerolaLogger.i(TAG, "Starting extraction for comic: $comicId", LogSource.SERVICE)
                val directory =
                    directoryDao.getDirectoryById(comicId)
                        ?: return@withContext IoError.FileNotFound("Comic directory not found in DB").left().also {
                            AcerolaLogger.e(TAG, "Comic $comicId not found", LogSource.SERVICE)
                        }

                val chapters = chapterArchiveDao.getChaptersByDirectoryId(comicId).first()
                val firstChapter =
                    chapters.firstOrNull()?.chapter
                        ?: return@withContext IoError.FileNotFound("No chapters found for this comic").left().also {
                            AcerolaLogger.e(TAG, "No chapters found for comic $comicId", LogSource.SERVICE)
                        }

                val folderUri = directory.path.toUri()
                val folderDoc =
                    DocumentFile.fromTreeUri(context, folderUri) ?: DocumentFile.fromSingleUri(context, folderUri)
                        ?: return@withContext IoError.FileReadError(directory.path, Exception("Could not resolve folder document")).left().also {
                            AcerolaLogger.e(TAG, "Could not resolve folder: ${directory.path}", LogSource.SERVICE)
                        }

                extractAndSaveCover(
                    chapter = firstChapter,
                    folderDoc = folderDoc,
                    onSuccess = {
                        AcerolaLogger.i(TAG, "Comic cover updated successfully", LogSource.SERVICE)
                        directoryDao.update(directory.copy(lastModified = System.currentTimeMillis()))
                    },
                )
            }

        suspend fun extractVolumeCover(
            comicId: Long,
            volumeId: Long,
        ): Either<IoError, Unit> =
            withContext(Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Starting extraction for volume: $volumeId (Comic: $comicId)", LogSource.SERVICE)
                val volume =
                    volumeArchiveDao.getVolumeById(volumeId)
                        ?: return@withContext IoError.FileNotFound("Volume not found in DB").left().also {
                            AcerolaLogger.e(TAG, "Volume $volumeId not found", LogSource.SERVICE)
                        }

                val chapters =
                    chapterArchiveDao.getChaptersByVolumePaged(
                        comicId = comicId,
                        volumeId = volumeId,
                        pageSize = 1,
                        offset = 0,
                    )

                val firstChapter =
                    chapters.firstOrNull()?.chapter
                        ?: return@withContext IoError.FileNotFound("No chapters found for this volume").left().also {
                            AcerolaLogger.e(TAG, "No chapters found for volume $volumeId", LogSource.SERVICE)
                        }

                val folderUri = volume.path.toUri()
                val folderDoc =
                    DocumentFile.fromTreeUri(context, folderUri) ?: DocumentFile.fromSingleUri(context, folderUri)
                        ?: return@withContext IoError.FileReadError(volume.path, Exception("Could not resolve volume folder document")).left().also {
                            AcerolaLogger.e(TAG, "Could not resolve volume folder: ${volume.path}", LogSource.SERVICE)
                        }

                extractAndSaveCover(
                    chapter = firstChapter,
                    folderDoc = folderDoc,
                    onSuccess = {
                        AcerolaLogger.i(TAG, "Volume cover updated successfully", LogSource.SERVICE)
                        volumeArchiveDao.update(volume.copy(lastModified = System.currentTimeMillis()))
                    },
                )
            }

        private suspend fun extractAndSaveCover(
            chapter: ChapterArchive,
            folderDoc: DocumentFile,
            onSuccess: suspend () -> Unit,
        ): Either<IoError, Unit> {
            val chapterDto = chapter.toViewDto()
            AcerolaLogger.d(TAG, "Extracting from chapter: ${chapter.chapter}", LogSource.SERVICE)

            return chapterSourceFactory
                .create(chapterDto)
                .mapLeft {
                    AcerolaLogger.e(TAG, "Failed to create chapter source", LogSource.SERVICE)
                    IoError.FileReadError(chapterDto.path, Exception(it.toString()))
                }.flatMap { source ->
                    try {
                        source
                            .openPage(0)
                            .mapLeft {
                                AcerolaLogger.e(TAG, "Failed to open page 0", LogSource.SERVICE)
                                IoError.FileReadError(chapterDto.path, Exception(it.toString()))
                            }.flatMap { inputStream ->
                                val bitmap =
                                    BitmapFactory.decodeStream(inputStream)
                                        ?: return@flatMap IoError
                                            .FileReadError(
                                                chapterDto.path,
                                                Exception("Failed to decode bitmap"),
                                            ).left().also {
                                                AcerolaLogger.e(TAG, "Failed to decode bitmap from input stream", LogSource.SERVICE)
                                            }

                                val outputStream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

                                val bytes = outputStream.toByteArray()
                                bitmap.recycle()

                                folderDoc.listFiles().forEach { file ->
                                    val fileName = file.name ?: return@forEach
                                    if (MediaFile.isCover(fileName)) {
                                        AcerolaLogger.d(TAG, "Deleting old cover: $fileName", LogSource.SERVICE)
                                        file.delete()
                                    }
                                }

                                AcerolaLogger.i(TAG, "Saving new cover: ${MediaFile.COVER.defaultFileName}", LogSource.SERVICE)
                                fileStorageHandler
                                    .saveFile(
                                        folder = folderDoc,
                                        fileName = MediaFile.COVER.defaultFileName,
                                        mimeType = "image/jpeg",
                                        bytes = bytes,
                                    ).also { result ->
                                        if (result.isRight()) {
                                            onSuccess()
                                        } else {
                                            AcerolaLogger.e(TAG, "Failed to save file: $result", LogSource.SERVICE)
                                        }
                                    }
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
