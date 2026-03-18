package br.acerola.manga.service.compact

import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.error.message.IoError

interface ArchiveCompactService {

    suspend fun createCbz(
        folder: DocumentFile,
        fileName: String,
        pageEntries: List<Pair<String, ByteArray>>
    ): Either<IoError, Unit>

    suspend fun saveImage(
        folder: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Either<IoError, Unit>
}