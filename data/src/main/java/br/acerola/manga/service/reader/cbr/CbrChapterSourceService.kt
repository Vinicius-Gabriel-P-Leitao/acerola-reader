package br.acerola.manga.service.reader.cbr

import android.content.Context
import android.net.Uri
import br.acerola.manga.dto.archive.ChapterFileDto
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
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

// TODO: Estudar mais as libs e fazer uma otimização e organização da busca desses dados
@Singleton
class CbrChapterSourceService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ChapterSourceService {

    private lateinit var archive: Archive
    private lateinit var entries: List<FileHeader>

    private val mutex = Mutex()


    fun open(chapter: ChapterFileDto): ChapterSourceService {
        // Close previous archive if it exists to release file handle
        try {
            archive.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val file = resolveFile(chapter.path)

        val newArchive = Archive(file)

        // Filter and sort entries
        val headers = newArchive.fileHeaders
            .filter { !it.isDirectory }
            .filter {
                val name = it.fileName.lowercase()
                name.endsWith(".jpg") ||
                        name.endsWith(".jpeg") ||
                        name.endsWith(".png") ||
                        name.endsWith(".webp")
            }
            .sortedBy { it.fileName }

        this.archive = newArchive
        this.entries = headers

        return this
    }

    override suspend fun pageCount(): Int = entries.size

    override suspend fun openPage(index: Int): InputStream {
        // We lock execution so only one extraction happens at a time
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val localArchive = archive ?: error("Archive not open")
                val header = entries.getOrNull(index) ?: error("Invalid page index: $index")

                val output = ByteArrayOutputStream()
                // extractFile relies on an internal file pointer that must not move during extraction
                localArchive.extractFile(header, output)

                ByteArrayInputStream(output.toByteArray())
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