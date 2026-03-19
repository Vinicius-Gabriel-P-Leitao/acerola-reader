package br.acerola.manga.adapter.port

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError

interface DownloadPort {

    suspend fun getMangaById(id: String): Either<NetworkError, MangaRemoteInfoDto>

    suspend fun searchMangaByTitle(
        title: String, limit: Int = 10
    ): Either<NetworkError, List<MangaRemoteInfoDto>>

    suspend fun getChaptersByLanguage(
        mangaId: String, language: String, limit: Int = 100, offset: Int = 0
    ): Either<NetworkError, Pair<List<ChapterRemoteInfoDto>, Int>>
}