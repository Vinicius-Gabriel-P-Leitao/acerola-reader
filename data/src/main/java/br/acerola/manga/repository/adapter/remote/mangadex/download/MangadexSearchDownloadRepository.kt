package br.acerola.manga.repository.adapter.remote.mangadex.download

import android.content.Context
import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.api.MangadexMangaInfoApi
import br.acerola.manga.repository.port.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexSearchDownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mangaInfoApi: MangadexMangaInfoApi,
    private val chapterInfoApi: MangadexChapterInfoApi,
) : DownloadRepository {

    override suspend fun searchMangaByTitle(
        title: String,
        limit: Int
    ): Either<NetworkError, List<MangaRemoteInfoDto>> = safeApiCall {
        withContext(Dispatchers.IO) {
            AcerolaLogger.d(TAG, "Searching MangaDex by title: $title", LogSource.NETWORK)
            val response = mangaInfoApi.searchMangaByName(title = title, limit = limit)
            response.data.map { it.toDto(context) }
        }
    }

    override suspend fun getMangaById(id: String): Either<NetworkError, MangaRemoteInfoDto> = safeApiCall {
        withContext(Dispatchers.IO) {
            AcerolaLogger.d(TAG, "Fetching MangaDex manga by ID: $id", LogSource.NETWORK)
            val response = mangaInfoApi.getMangaById(mangaId = id)
            response.data.toDto(context)
        }
    }

    override suspend fun getChaptersByLanguage(
        mangaId: String,
        language: String,
        limit: Int,
        offset: Int
    ): Either<NetworkError, Pair<List<ChapterRemoteInfoDto>, Int>> = safeApiCall {
        withContext(Dispatchers.IO) {
            AcerolaLogger.d(TAG, "Fetching chapters for manga $mangaId in $language (offset: $offset)", LogSource.NETWORK)
            val response = chapterInfoApi.getMangaFeed(
                mangaId = mangaId,
                languages = listOf(language),
                limit = limit,
                offset = offset
            )
            val chapters = response.data.map { it.toDto() }
            AcerolaLogger.i(TAG, "Got ${chapters.size} chapters (total: ${response.total})", LogSource.NETWORK)
            chapters to response.total
        }
    }

    companion object {
        private const val TAG = "MangadexSearchDownloadRepository"
    }
}
