package br.acerola.manga.core.usecase.search

import arrow.core.Either
import br.acerola.manga.pattern.MangadexPattern
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.adapter.di.MangadexSource
import br.acerola.manga.adapter.port.DownloadPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchMangaUseCase @Inject constructor(
    @param:MangadexSource private val downloadPort: DownloadPort
) {
    suspend fun search(query: String): Either<NetworkError, List<MangaRemoteInfoDto>> {
        val mangadexId = extractMangadexId(query)

        return if (mangadexId != null) {
            downloadPort.getMangaById(mangadexId).map { listOf(it) }
        } else {
            downloadPort.searchMangaByTitle(query)
        }
    }

    suspend fun getChaptersByLanguage(
        mangaId: String,
        language: String,
        page: Int = 0,
        limit: Int = 100,
    ): Either<NetworkError, Pair<List<ChapterRemoteInfoDto>, Int>> =
        downloadPort.getChaptersByLanguage(mangaId, language, limit, page * limit)

    private fun extractMangadexId(query: String): String? {
        MangadexPattern.titleUrl.find(query)?.groupValues?.get(1)?.let { return it }
        if (MangadexPattern.uuid.matches(query)) return query
        return null
    }
}
