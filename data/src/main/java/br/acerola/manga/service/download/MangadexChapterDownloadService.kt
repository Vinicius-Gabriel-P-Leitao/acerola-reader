package br.acerola.manga.service.download

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.config.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterDownloadService @Inject constructor(
    private val chapterInfoApi: MangadexChapterInfoApi,
    private val downloadApi: MangadexDownloadApi
) : ChapterDownloadService {

    override suspend fun listChaptersByLanguage(
        mangaId: String,
        language: String,
        limit: Int,
        offset: Int
    ): Either<NetworkError, Pair<List<ChapterRemoteInfoDto>, Int>> = safeApiCall {
        withContext(Dispatchers.IO) {
            val response = chapterInfoApi.getMangaFeed(
                mangaId = mangaId,
                languages = listOf(language),
                limit = limit,
                offset = offset
            )
            val chapters = response.data.map { it.toDto() }
            chapters to response.total
        }
    }

    override suspend fun getPageUrls(chapterId: String): Either<NetworkError, List<String>> =
        safeApiCall(timeoutMs = 500L) {
            withContext(Dispatchers.IO) {
                val source = chapterInfoApi.getChapterImages(chapterId)
                source.chapter.data.map { "${source.baseUrl}/data/${source.chapter.hash}/$it" }
            }
        }

    override suspend fun downloadBytes(url: String): ByteArray? =
        safeApiCall(timeoutMs = 200L) {
            withContext(Dispatchers.IO) {
                downloadApi.downloadFile(url).bytes()
            }
        }.getOrNull()
}
