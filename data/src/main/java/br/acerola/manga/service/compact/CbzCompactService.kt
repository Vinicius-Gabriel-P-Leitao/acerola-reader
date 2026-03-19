package br.acerola.manga.service.compact

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.error.message.IoError
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
    ): Either<IoError, Unit> {
        val file = folder.createFile("application/octet-stream", fileName)
            ?: return Either.Left(IoError.FileWriteError(path = fileName))

        return Either.catch {
            context.contentResolver.openOutputStream(file.uri)?.use { outStream ->
                ZipOutputStream(outStream).use { zip ->
                    pageEntries.forEach { (entryName, bytes) ->
                        zip.putNextEntry(ZipEntry(entryName))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
            }
            Unit
        }.mapLeft { cause ->
            file.delete()
            IoError.FileWriteError(path = fileName, cause = cause) as IoError
        }
    }

    override suspend fun saveImage(
        folder: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Either<IoError, Unit> {
        val file = folder.createFile(mimeType, fileName)
            ?: return Either.Left(IoError.FileWriteError(path = fileName))

        return Either.catch {
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
            Unit
        }.mapLeft { cause ->
            file.delete()
            IoError.FileWriteError(path = fileName, cause = cause) as IoError
        }
    }
}
