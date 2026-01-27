package br.acerola.manga.service.reader.cbr

import android.content.Context
import android.net.Uri
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.service.reader.port.ChapterSourceService
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CbrChapterSourceService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ChapterSourceService {

    private lateinit var archive: Archive
    private lateinit var entries: List<FileHeader>

    fun open(chapter: ChapterFileDto): ChapterSourceService {
        val file = resolveFile(chapter.path)

        archive = Archive(file)

        entries = archive.fileHeaders
            .filter { !it.isDirectory }
            .filter {
                val name = it.fileName.lowercase()
                name.endsWith(".jpg") ||
                        name.endsWith(".jpeg") ||
                        name.endsWith(".png") ||
                        name.endsWith(".webp")
            }
            .sortedBy { it.fileName }

        return this
    }

    override suspend fun pageCount(): Int = entries.size

    override suspend fun openPage(index: Int): InputStream {
        val header = entries.getOrNull(index) ?: error("Página $index inválida")

        val output = ByteArrayOutputStream()
        archive.extractFile(header, output)

        return ByteArrayInputStream(output.toByteArray())
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open URI: $path")

            val tempFile = File(context.cacheDir, "temp_chapter_read.cbr")
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