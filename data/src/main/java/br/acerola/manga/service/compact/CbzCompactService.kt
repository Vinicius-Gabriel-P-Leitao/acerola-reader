package br.acerola.manga.service.compact

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.service.compact.ArchiveCompactService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CbzCompactService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ArchiveCompactService {

    override suspend fun createCbz(
        folder: DocumentFile,
        fileName: String,
        pageEntries: List<Pair<String, ByteArray>>
    ): Boolean {
        val file = folder.createFile("application/octet-stream", fileName) ?: return false
        return try {
            context.contentResolver.openOutputStream(file.uri)?.use { outStream ->
                ZipOutputStream(outStream).use { zip ->
                    pageEntries.forEach { (entryName, bytes) ->
                        zip.putNextEntry(ZipEntry(entryName))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            file.delete()
            false
        }
    }

    override suspend fun saveImage(
        folder: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        val file = folder.createFile(mimeType, fileName) ?: return false
        return try {
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
            true
        } catch (e: Exception) {
            file.delete()
            false
        }
    }
}
