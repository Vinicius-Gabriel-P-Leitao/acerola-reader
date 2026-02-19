package br.acerola.manga.service.reader.extract

import android.content.Context
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.reader.port.ChapterSourceService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Estudar mais as libs e fazer uma otimização e organização da busca desses dados
@Singleton
class CbzChapterSourceService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ChapterSourceService {

    private lateinit var zipFile: ZipFile
    private lateinit var entries: List<ZipEntry>

    override suspend fun pageCount(): Int = entries.size

    override suspend fun openPage(index: Int): Either<ChapterError, InputStream> {
        return Either.Companion.catch { zipFile.getInputStream(entries[index]) }
            .mapLeft { exception ->
                ChapterError.ExtractionFailed(cause = exception)
            }
    }

    override suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream> {
        return Either.catch {
            val entry = zipFile.getEntry(fileName)
                ?: throw FileNotFoundException("File $fileName not found in ZIP")
            zipFile.getInputStream(entry)
        }.mapLeft { exception ->
            when (exception) {
                is FileNotFoundException -> ChapterError.InvalidChapterData(reason = exception.message ?: "File not found")
                else -> ChapterError.ExtractionFailed(cause = exception)
            }
        }
    }

    override fun open(chapter: ChapterFileDto): Either<ChapterError, ChapterSourceService> {
        return Either.catch {
            val file = resolveFile(chapter.path)

            zipFile = ZipFile(file)
            entries = zipFile.entries().toList()
                .filter { !it.isDirectory }
                .filter {
                    val name = it.name.lowercase()
                    // TODO: Fazer isso como pattern matching
                    name.endsWith(".jpg") ||
                            name.endsWith(".jpeg") ||
                            name.endsWith(".png") ||
                            name.endsWith(".webp")
                }
                .sortedBy { it.name }

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
            // It's a SAF URI, we need to copy it to a temp file to read it as a ZipFile
            val uri = path.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open URI: $path")

            val tempFile = File(context.cacheDir, "temp_chapter_read.cbz")
            // Create/Overwrite the temp file
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            tempFile
        } else {
            // It's a standard file path
            File(path)
        }
    }
}