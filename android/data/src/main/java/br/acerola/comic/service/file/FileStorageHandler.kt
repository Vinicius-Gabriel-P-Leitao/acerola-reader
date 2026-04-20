package br.acerola.comic.service.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.left
import br.acerola.comic.error.message.IoError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageHandler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun saveFile(
            folder: DocumentFile,
            fileName: String,
            mimeType: String,
            bytes: ByteArray,
        ): Either<IoError, Unit> =
            withContext(Dispatchers.IO) {
                val existingFile = folder.findFile(fileName)
                val file =
                    existingFile ?: folder.createFile(mimeType, fileName)

                        ?: return@withContext IoError
                            .FileWriteError(
                                path = fileName,
                                cause = Exception("Could not create file in folder: ${folder.uri}. Ensure it's a writable Tree Document."),
                            ).left()

                Either
                    .catch {
                        context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(bytes) }
                        Unit
                    }.mapLeft { cause ->
                        if (existingFile == null) file.delete()
                        IoError.FileWriteError(path = fileName, cause = cause)
                    }
            }
    }
