package br.acerola.manga.service.compact

import androidx.documentfile.provider.DocumentFile

interface ArchiveCompactService {

    suspend fun createCbz(
        folder: DocumentFile,
        fileName: String,
        pageEntries: List<Pair<String, ByteArray>>
    ): Boolean

    suspend fun saveImage(
        folder: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean
}