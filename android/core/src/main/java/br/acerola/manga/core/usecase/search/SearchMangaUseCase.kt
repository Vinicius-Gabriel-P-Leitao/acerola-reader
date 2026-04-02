package br.acerola.manga.core.usecase.search

import arrow.core.Either
import br.acerola.manga.adapter.contract.provider.DownloadProvider
import br.acerola.manga.adapter.metadata.mangadex.MangadexSource
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.pattern.MangadexPattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchMangaUseCase @Inject constructor(
    @param:MangadexSource private val downloadProvider: DownloadProvider
) {
    suspend fun search(query: String): Either<NetworkError, List<MangaMetadataDto>> {
        val mangadexId = extractMangadexId(query)

        return if (mangadexId != null) downloadProvider.getMangaById(mangadexId).map { listOf(it) }
        else downloadProvider.searchMangaByTitle(query)

    }

    suspend fun getChaptersByLanguage(
        mangaId: String,
        language: String?,
        page: Int = 0,
        limit: Int = 100,
    ): Either<NetworkError, Pair<List<ChapterMetadataDto>, Int>> =
        downloadProvider.getChaptersByLanguage(mangaId, language, limit, page * limit)

    private fun extractMangadexId(query: String): String? {
        MangadexPattern.titleUrl.find(query)?.groupValues?.get(1)?.let { return it }
        if (MangadexPattern.uuid.matches(query)) return query
        return null
    }
}
