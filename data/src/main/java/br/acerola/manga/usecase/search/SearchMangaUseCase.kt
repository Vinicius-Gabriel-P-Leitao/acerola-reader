package br.acerola.manga.usecase.search

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.repository.port.DownloadRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchMangaUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend fun search(query: String): Either<NetworkError, List<MangaRemoteInfoDto>> {
        val mangadexId = extractMangadexId(query)
        return if (mangadexId != null) {
            downloadRepository.getMangaById(mangadexId).map { listOf(it) }
        } else {
            downloadRepository.searchMangaByTitle(query)
        }
    }

    suspend fun getChaptersByLanguage(
        mangaId: String,
        language: String
    ): Either<NetworkError, List<ChapterRemoteInfoDto>> =
        downloadRepository.getChaptersByLanguage(mangaId, language).map { (chapters, _) -> chapters }

    private fun extractMangadexId(query: String): String? {
        val urlRegex = Regex("mangadex\\.org/title/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
        urlRegex.find(query)?.groupValues?.get(1)?.let { return it }
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
        if (uuidRegex.matches(query)) return query
        return null
    }
}
