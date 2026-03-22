package br.acerola.manga.service.compact

import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.error.message.IoError

interface PdfToCbzConverterService {
    suspend fun convertPdfToCbz(
        folder: DocumentFile,
        pdfFile: DocumentFile,
        cbzFileName: String
    ): Either<IoError, Unit>
}
