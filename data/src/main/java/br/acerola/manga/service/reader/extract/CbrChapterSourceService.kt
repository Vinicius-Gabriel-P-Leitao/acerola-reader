package br.acerola.manga.service.reader.extract

import android.content.Context
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.left
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.reader.port.ChapterSourceService
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Estudar mais as libs e fazer uma otimização e organização da busca desses dados
@Singleton
class CbrChapterSourceService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ChapterSourceService {
    private lateinit var archive: Archive
    private lateinit var entries: List<FileHeader>
    private val mutex = Mutex()

    override suspend fun pageCount(): Int = entries.size

    override suspend fun openPage(index: Int): Either<ChapterError, InputStream> = mutex.withLock {
        withContext(context = Dispatchers.IO) {
            val localArchive = archive

            val header = entries.getOrNull(index)
                ?: return@withContext ChapterError.InvalidChapterData("Index $index out of bounds").left()

            Either.catch {
                val output = ByteArrayOutputStream()
                localArchive.extractFile(header, output)
                ByteArrayInputStream(output.toByteArray())
            }.mapLeft {
                ChapterError.ExtractionFailed(cause = it)
            }
        }
    }

    override suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val header = archive.fileHeaders.find { it.fileName.equals(fileName, ignoreCase = true) }
                ?: return@withContext ChapterError.InvalidChapterData("File $fileName not found in RAR").left()

            Either.catch {
                val output = ByteArrayOutputStream()
                archive.extractFile(header, output)
                ByteArrayInputStream(output.toByteArray())
            }.mapLeft {
                ChapterError.ExtractionFailed(cause = it)
            }
        }
    }

    override fun open(chapter: ChapterFileDto): Either<ChapterError, ChapterSourceService> {
        return Either.catch {
            val file = resolveFile(chapter.path)
            val newArchive = Archive(file)

            val headers = newArchive.fileHeaders
                .filter { !it.isDirectory }
                .filter {
                    val name = it.fileName.lowercase()
                    // TODO: Fazer isso como pattern matching
                    name.endsWith(".jpg") ||
                            name.endsWith(".jpeg") ||
                            name.endsWith(".png") ||
                            name.endsWith(".webp")
                }.sortedBy { it.fileName }

            runCatching { archive.close() }
            this.archive = newArchive
            this.entries = headers

            this
        }.mapLeft { error ->
            when (error) {
                is FileNotFoundException -> ChapterError.ArchiveNotFound(chapter.path)
                else -> ChapterError.ArchiveCorrupted(chapter.path, error)
            }
        }
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("content://")) {
            val uri = path.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open URI: $path")

            // Create a temp file in cache
            val tempFile = File(context.cacheDir, "temp_chapter_read.cbr")

            // Overwrite existing temp file
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            tempFile
        } else {
            File(path)
        }
    }
}