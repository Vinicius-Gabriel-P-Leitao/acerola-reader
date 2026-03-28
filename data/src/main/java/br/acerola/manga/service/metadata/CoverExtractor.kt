package br.acerola.manga.service.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import br.acerola.manga.error.message.IoError
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.translator.ui.toViewDto
import br.acerola.manga.pattern.MediaFilePattern
import br.acerola.manga.service.file.FileStorageHandler
import br.acerola.manga.service.reader.ChapterSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val fileStorageHandler: FileStorageHandler,
    private val chapterSourceFactory: ChapterSourceFactory,
) {

    suspend fun extractFirstPageAsCover(mangaId: Long): Either<IoError, Unit> = withContext(Dispatchers.IO) {
        val directory = directoryDao.getMangaDirectoryById(mangaId)
            ?: return@withContext IoError.FileNotFound("Manga directory not found in DB").left()

        val chapters = chapterArchiveDao.getChaptersByMangaDirectory(mangaId).first()
        val firstChapter = chapters.minByOrNull { it.chapterSort.toDoubleOrNull() ?: Double.MAX_VALUE }
            ?: return@withContext IoError.FileNotFound("No chapters found for this manga").left()

        val folderUri = directory.path.toUri()
        val folderDoc = DocumentFile.fromTreeUri(context, folderUri) ?: DocumentFile.fromSingleUri(context, folderUri)
            ?: return@withContext IoError.FileReadError(directory.path, Exception("Could not resolve folder document")).left()

        val chapterDto = firstChapter.toViewDto()

        chapterSourceFactory.create(chapterDto).mapLeft { 
            IoError.FileReadError(chapterDto.path, Exception(it.toString())) 
        }.flatMap { source ->
            try {
                source.openPage(0).mapLeft { 
                    IoError.FileReadError(chapterDto.path, Exception(it.toString())) 
                }.flatMap { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                        ?: return@flatMap IoError.FileReadError(chapterDto.path, Exception("Failed to decode bitmap")).left()
                    
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val bytes = outputStream.toByteArray()
                    bitmap.recycle()

                    fileStorageHandler.saveFile(
                        folder = folderDoc,
                        fileName = MediaFilePattern.COVER.defaultFileName,
                        mimeType = "image/png",
                        bytes = bytes
                    ).also { result ->
                        if (result.isRight()) {
                            directoryDao.update(directory.copy(lastModified = System.currentTimeMillis()))
                        }
                    }
                }
            } finally {
                source.close()
            }
        }
    }
}
