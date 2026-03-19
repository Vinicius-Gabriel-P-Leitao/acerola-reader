package br.acerola.manga.service.download

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.NetworkError

interface ChapterDownloadService {

    suspend fun listChaptersByLanguage(
        mangaId: String,
        language: String,
        limit: Int = 500,
        offset: Int = 0
    ): Either<NetworkError, Pair<List<ChapterRemoteInfoDto>, Int>>

    suspend fun getPageUrls(chapterId: String): Either<NetworkError, List<String>>

    suspend fun downloadBytes(url: String): ByteArray?
}