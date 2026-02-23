package br.acerola.manga.repository.adapter.remote.mangadex.chapter

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterInfoRepository @Inject constructor(
    private val api: MangadexChapterInfoApi
) : RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, onProgress: ((Int) -> Unit)?, vararg extra: String?
    ): Either<NetworkError, List<ChapterRemoteInfoDto>> = withContext(context = Dispatchers.IO) {
        val allChapters = mutableListOf<ChapterRemoteInfoDto>()
        val semaphore = Semaphore(permits = 3)
        var currentOffset = offset

        var error: NetworkError? = null

        // Initial fetch to get total
        val initialResponseResult = safeApiCall { api.getMangaFeed(mangaId = manga, limit = 1, offset = 0) }
        val totalChapters = initialResponseResult.getOrNull()?.total ?: 0

        while (true) {
            println("DEBUG API: Fetching Feed Batch - Offset=$currentOffset | Limit=$limit")

            if (totalChapters > 0 && onProgress != null) {
                val progress = ((currentOffset.toFloat() / totalChapters.toFloat()) * 100).toInt()
                onProgress(progress)
            }

            val responseFeedResult = safeApiCall {
                api.getMangaFeed(mangaId = manga, limit = limit, offset = currentOffset)
            }

            if (responseFeedResult is Either.Left) {
                error = responseFeedResult.value
                break
            }

            val responseFeed = responseFeedResult.getOrNull()!!
            val chaptersData = responseFeed.data

            val processedBatch = chaptersData.map { item ->
                async {
                    semaphore.withPermit {
                        val sourceResult = safeApiCall { api.getChapterImages(chapterId = item.id) }
                        val source = sourceResult.getOrNull()

                        if (source == null) {
                            // TODO: Dar um callback visual depois
                            println("DEBUG API: [!] Sem imagens para o chapter ${item.attributes.chapter}")
                        }

                        item.toDto(source)
                    }
                }
            }.awaitAll()

            allChapters.addAll(elements = processedBatch)
            currentOffset += 100

            if (currentOffset >= responseFeed.total) {
                // TODO: Dar um callback visual depois
                println("DEBUG API: Finalizou $currentOffset chapters processados.")
                break
            }
        }

        if (error != null && allChapters.isEmpty()) {
            Either.Left(value = error)
        } else {
            Either.Right(value = allChapters)
        }
    }

    override suspend fun saveInfo(manga: String, info: ChapterRemoteInfoDto): Either<br.acerola.manga.error.message.NetworkError, Unit> {
        // NOTE: MangaDex é apenas leitura para nós
        return Either.Right(Unit)
    }
}