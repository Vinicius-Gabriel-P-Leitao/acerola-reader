package br.acerola.manga.service.reader.extract

import android.content.Context
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.left
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.reader.contract.PageSource
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class CbrPageResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PageSource {

    private var archive: Archive? = null
    private var entries: List<FileHeader> = emptyList()
    private var currentTempFile: File? = null
    private val mutex = Mutex()

    override suspend fun pageCount(): Int = entries.size

    override suspend fun openPage(index: Int): Either<ChapterError, InputStream> = mutex.withLock {
        withContext(context = Dispatchers.IO) {
            val localArchive = archive ?: return@withContext ChapterError.InvalidChapterData("Archive not open").left()

            val header = entries.getOrNull(index)
                ?: return@withContext ChapterError.InvalidChapterData("Index $index out of bounds").left()

            Either.catch {
                localArchive.getInputStream(header)
            }.mapLeft { exception ->
                ChapterError.ExtractionFailed(cause = exception)
            }
        }
    }

    override suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream> = mutex.withLock {
        withContext(context = Dispatchers.IO) {
            val localArchive = archive ?: return@withContext ChapterError.InvalidChapterData("Archive not open").left()
            val header = localArchive.fileHeaders.find { it.fileName.equals(fileName, ignoreCase = true) }
                ?: return@withContext ChapterError.InvalidChapterData("File $fileName not found in RAR").left()

            Either.catch {
                localArchive.getInputStream(header)
            }.mapLeft { exception ->
                ChapterError.ExtractionFailed(cause = exception)
            }
        }
    }

    override fun open(chapter: ChapterFileDto): Either<ChapterError, PageSource> {
        return Either.catch {
            close() // NOTE: Limpa o anterior antes de abrir um novo

            val file = resolveFile(chapter.path)
            val newArchive = Archive(file)

            val headers = newArchive.fileHeaders
                .filter { !it.isDirectory }
                .filter {
                    val name = it.fileName.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")

                }.sortedBy { it.fileName }

            this.archive = newArchive
            this.entries = headers

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
            archive?.close()
        } catch (exception: Exception) {
            // Ignora erros ao fechar
        } finally {
            archive = null
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
            val tempFile = File.createTempFile("chapter_sync_", ".cbr", context.cacheDir)
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
