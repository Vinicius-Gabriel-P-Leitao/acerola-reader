package br.acerola.comic.service.reader.extract

import android.content.Context
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.left
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.error.message.ChapterError
import br.acerola.comic.service.reader.contract.PageSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

class CbzPageResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PageSource {

    private var zipFile: ZipFile? = null
    private var entries: List<ZipEntry> = emptyList()
    private var currentTempFile: File? = null

    override suspend fun pageCount(): Int = entries.size

    override suspend fun openPage(index: Int): Either<ChapterError, InputStream> {
        val localZip = zipFile ?: return ChapterError.InvalidChapterData("Zip file not open").left()
        return Either.Companion.catch { localZip.getInputStream(entries[index]) }
            .mapLeft { exception ->
                ChapterError.ExtractionFailed(cause = exception)
            }
    }

    override suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream> {
        val localZip = zipFile ?: return ChapterError.InvalidChapterData("Zip file not open").left()
        return Either.catch {
            val entry = localZip.getEntry(fileName)
                ?: throw FileNotFoundException("File $fileName not found in ZIP")
            localZip.getInputStream(entry)
        }.mapLeft { exception ->
            when (exception) {
                is FileNotFoundException -> ChapterError.InvalidChapterData(
                    reason = exception.message ?: "File not found"
                )
                else -> ChapterError.ExtractionFailed(cause = exception)
            }
        }
    }

    override fun open(chapter: ChapterFileDto): Either<ChapterError, PageSource> {
        return Either.catch {
            close() // NOTE: Limpa o anterior antes de abrir um novo

            val file = resolveFile(chapter.path)

            val newZipFile = ZipFile(file)
            val newEntries = newZipFile.entries().toList()
                .filter { !it.isDirectory }
                .filter {
                    val name = it.name.lowercase()
                    name.endsWith(".jpg") ||
                            name.endsWith(".jpeg") ||
                            name.endsWith(".png") ||
                            name.endsWith(".webp")
                }
                .sortedBy { it.name }

            this.zipFile = newZipFile
            this.entries = newEntries

            this
        }.mapLeft { exception ->
            when (exception) {
                is FileNotFoundException -> ChapterError.ArchiveNotFound(chapter.path)
                else -> ChapterError.ArchiveCorrupted(chapter.path, exception)
            }
        }
    }

    override fun close() {
        try {
            zipFile?.close()
        } catch (exception: Exception) {
            // Ignora erros ao fechar
        } finally {
            zipFile = null
            entries = emptyList()
            currentTempFile?.delete()
            currentTempFile = null
        }
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("content://")) {
            val uri = path.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open URI: $path")

            // Cria um arquivo temporário único para evitar concorrência (SIGBUS)
            val tempFile = File.createTempFile("chapter_sync_", ".cbz", context.cacheDir)
            currentTempFile = tempFile

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