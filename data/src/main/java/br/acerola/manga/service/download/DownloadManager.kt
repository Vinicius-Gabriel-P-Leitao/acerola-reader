package br.acerola.manga.service.download

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.error.message.NetworkError

interface DownloadManager {

    suspend fun listChaptersByLanguage(
        mangaId: String,
        language: String,
        limit: Int = 500,
        offset: Int = 0
    ): Either<NetworkError, Pair<List<ChapterMetadataDto>, Int>>

    suspend fun getPageUrls(chapterId: String): Either<NetworkError, List<String>>

    suspend fun downloadBytes(url: String): ByteArray?
}