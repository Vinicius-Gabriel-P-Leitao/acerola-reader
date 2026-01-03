package br.acerola.manga.repository.adapter.remote.mangadex.chapter

import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.manga.repository.port.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterInfoService @Inject constructor(
    private val api: MangadexChapterInfoApi
) : ApiRepository.RemoteInfoOperations<ChapterRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, vararg extra: String?
    ): List<ChapterRemoteInfoDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val responseFeed = api.getMangaFeed(mangaId = manga, limit = limit, offset = offset)
                println("Response $responseFeed")
                val chaptersRemoteInfoList = responseFeed.data

                val deferredChapters = chaptersRemoteInfoList.map {
                    async {
                        val sourceMangadexDto = api.getChapterImages(chapterId = it.id)
                        fromChapterData(remoteInfoDto = it, sourceMangadexDto)
                    }
                }

                deferredChapters.awaitAll()
            } catch (httpException: HttpException) {
                throw MangadexRequestException(
                    title = R.string.title_http_error,
                    // TODO: Fazer isso de forma global, fazer um wrapper que pega erros http e lança a exception que
                    //  quero
                    description = if (httpException.code() == 429) R.string.description_http_error_rate_limit
                    else R.string.description_http_error_generic
                )
            } catch (exception: Exception) {
                // TODO: Tratar erro melhor
                println("Erro ao sincronizar $exception")
                throw MangadexRequestException(
                    title = R.string.title_remote_info_request_error,
                    description = R.string.description_remote_info_request_error
                )
            }
        }
    }

    private fun fromChapterData(
        remoteInfoDto: ChapterMangadexDto,
        sourceMangadexDto: ChapterSourceMangadexDto? = null
    ): ChapterRemoteInfoDto {
        val attributes = remoteInfoDto.attributes
        val scanlatorName = remoteInfoDto.scanlationGroups.firstNotNullOfOrNull { it.attributes?.name }

        val pagesUrls = if (sourceMangadexDto != null) {
            val dataSaver = sourceMangadexDto.chapter
            val baseUrl = sourceMangadexDto.baseUrl
            val hash = dataSaver.hash

            dataSaver.data.map { fileName ->
                "$baseUrl/data/$hash/$fileName"
            }
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