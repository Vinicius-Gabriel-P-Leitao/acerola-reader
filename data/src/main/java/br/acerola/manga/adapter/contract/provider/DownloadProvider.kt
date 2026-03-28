package br.acerola.manga.adapter.contract.provider

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError

interface DownloadProvider {

    suspend fun getMangaById(id: String): Either<NetworkError, MangaMetadataDto>

    suspend fun searchMangaByTitle(
        title: String, limit: Int = 10
    ): Either<NetworkError, List<MangaMetadataDto>>

    suspend fun getChaptersByLanguage(
        mangaId: String, language: String, limit: Int = 100, offset: Int = 0
    ): Either<NetworkError, Pair<List<ChapterMetadataDto>, Int>>
}
