package br.acerola.manga.source.adapter.mangadex.chapter

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.config.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.source.port.RemoteInfoOperationsPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterInfoPort @Inject constructor(
    private val api: MangadexChapterInfoApi
) : RemoteInfoOperationsPort<ChapterRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, onProgress: ((Int) -> Unit)?, vararg extra: String?
    ): Either<NetworkError, List<ChapterRemoteInfoDto>> = withContext(context = Dispatchers.IO) {
        AcerolaLogger.d(TAG, "GET /manga/$manga/feed initiated (offset: $offset)", LogSource.NETWORK)  
        val allChapters = mutableListOf<ChapterRemoteInfoDto>()
        val semaphore = Semaphore(permits = 3)
        var currentOffset = offset

        var error: NetworkError? = null

        // Initial fetch to get total
        val initialResponseResult = safeApiCall { api.getMangaFeed(mangaId = manga, limit = 1, offset = 0) }
        val totalChapters = initialResponseResult.getOrNull()?.total ?: 0

        while (true) {
            if (totalChapters > 0 && onProgress != null) {
                val progress = ((currentOffset.toFloat() / totalChapters.toFloat()) * 100).toInt()
                onProgress(progress)
            }

            val responseFeedResult = safeApiCall {
                api.getMangaFeed(mangaId = manga, limit = limit, offset = currentOffset)
            }

            if (responseFeedResult is Either.Left) {
                AcerolaLogger.e(TAG, "Failed to fetch chapter feed for manga: $manga", LogSource.NETWORK)  
                error = responseFeedResult.value
                break
            }

            val responseFeed = responseFeedResult.getOrNull()!!
            AcerolaLogger.d(TAG, "Feed received: ${responseFeed.data.size} chapters in this batch", LogSource.NETWORK)  
            val chaptersData = responseFeed.data

            val processedBatch = chaptersData.map { item ->
                async {
                    semaphore.withPermit {
                        val sourceResult = safeApiCall { api.getChapterImages(chapterId = item.id) }
                        val source = sourceResult.getOrNull()

                        item.toDto(source)
                    }
                }
            }.awaitAll()

            allChapters.addAll(elements = processedBatch)
            currentOffset += 100

            if (currentOffset >= responseFeed.total) {
                break
            }
        }

        if (error != null && allChapters.isEmpty()) {
            Either.Left(value = error)
        } else {
            AcerolaLogger.i(TAG, "Successfully fetched total of ${allChapters.size} chapters for manga $manga", LogSource.NETWORK)  
            Either.Right(value = allChapters)
        }
    }

    override suspend fun saveInfo(manga: String, info: ChapterRemoteInfoDto): Either<NetworkError, Unit> {
        return Either.Right(Unit)
    }

    companion object {
        private const val TAG = "MangadexChapterInfoRepository"  
    }
}
