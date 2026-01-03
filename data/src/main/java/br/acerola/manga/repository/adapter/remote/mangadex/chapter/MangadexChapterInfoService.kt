package br.acerola.manga.repository.adapter.remote.mangadex.chapter

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.manga.repository.port.ApiRepository
import br.acerola.manga.util.safeApiCall
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
    ): List<ChapterRemoteInfoDto> = safeApiCall {
        withContext(context = Dispatchers.IO) {
            val responseFeed = api.getMangaFeed(mangaId = manga, limit = limit, offset = offset)
            val chaptersRemoteInfoList = responseFeed.data

            val semaphore = Semaphore(permits = 5)

            val deferredChapters = chaptersRemoteInfoList.map { item ->
                async {
                    semaphore.withPermit {
                        try {
                            val source = api.getChapterImages(chapterId = item.id)
                            fromChapterData(remoteInfoDto = item, sourceMangadexDto = source)
                        } catch (_: Exception) {
                            fromChapterData(remoteInfoDto = item, sourceMangadexDto = null)
                        }
                    }
                }
            }

            deferredChapters.awaitAll()
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
            // TODO: Tratar erro melhor
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