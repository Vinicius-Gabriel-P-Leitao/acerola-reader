package br.acerola.manga.repository.adapter.remote.mangadex.chapter

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.manga.repository.port.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterInfoService @Inject constructor(
    private val api: MangadexChapterInfoApi
) : ApiRepository.RemoteInfoOperations<ChapterRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, vararg extra: String?
    ): Either<NetworkError, List<ChapterRemoteInfoDto>> = withContext(context = Dispatchers.IO) {
        val allChapters = mutableListOf<ChapterRemoteInfoDto>()
        val semaphore = Semaphore(permits = 3)
        var currentOffset = offset

        var error: NetworkError? = null

        while (true) {
            println("DEBUG API: Fetching Feed Batch - Offset=$currentOffset | Limit=$limit")

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
                            println("DEBUG API: [!] No images found or error for Chapter ${item.attributes.chapter}")
                        }

                        fromChapterData(remoteInfoDto = item, sourceMangadexDto = source)
                    }
                }
            }.awaitAll()

            allChapters.addAll(elements = processedBatch)
            currentOffset += 100

            if (currentOffset >= responseFeed.total) {
                // TODO: Dar um callback visual depois
                println("DEBUG API: Finished. All $currentOffset chapters processed.")
                break
            }
        }

        if (error != null && allChapters.isEmpty()) {
            Either.Left(value = error)
        } else {
            Either.Right(value = allChapters)
        }
    }


    private fun fromChapterData(
        remoteInfoDto: ChapterMangadexDto, sourceMangadexDto: ChapterSourceMangadexDto? = null
    ): ChapterRemoteInfoDto {
        val attributes = remoteInfoDto.attributes
        val scanlatorName = remoteInfoDto.scanlationGroups.firstNotNullOfOrNull { it.attributes?.name }

        val pagesUrls = if (sourceMangadexDto != null) {
            val dataSaver = sourceMangadexDto.chapter
            val baseUrl = sourceMangadexDto.baseUrl
            val hash = dataSaver.hash

            dataSaver.data.map { "$baseUrl/data/$hash/$it" }
        } else {
            emptyList()
        }

        return ChapterRemoteInfoDto(
            id = remoteInfoDto.id,
            volume = attributes.volume,
            chapter = attributes.chapter,
            title = attributes.title,
            scanlator = scanlatorName,
            pages = attributes.pages,
            mangadexVersion = remoteInfoDto.version,
            pageUrls = pagesUrls
        )
    }
}