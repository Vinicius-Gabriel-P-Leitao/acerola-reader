package br.acerola.manga.service.metadata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.IoError
import br.acerola.manga.service.reader.ChapterSourceFactory
import br.acerola.manga.service.file.FileStorageService
import br.acerola.manga.pattern.MediaFilePattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverExtractionService @Inject constructor(
    private val chapterSourceFactory: ChapterSourceFactory,
    private val fileStorageService: FileStorageService
) {

    suspend fun extractFirstPageAsCover(
        mangaFolder: DocumentFile,
        chapter: ChapterFileDto
    ): Either<IoError, Unit> = withContext(Dispatchers.IO) {
        chapterSourceFactory.create(chapter).mapLeft { 
            IoError.FileReadError(chapter.path, Exception(it.toString())) 
        }.flatMap { source ->
            try {
                source.openPage(0).mapLeft { 
                    IoError.FileReadError(chapter.path, Exception(it.toString())) 
                }.flatMap { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                        ?: return@flatMap IoError.FileReadError(chapter.path, Exception("Failed to decode bitmap")).left()
                    
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val bytes = outputStream.toByteArray()
                    bitmap.recycle()

                    fileStorageService.saveFile(
                        folder = mangaFolder,
                        fileName = MediaFilePattern.COVER.defaultFileName,
                        mimeType = "image/png",
                        bytes = bytes
                    )
                }
            } finally {
                source.close()
            }
        }
    }
}
