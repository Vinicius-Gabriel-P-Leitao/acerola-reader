package br.acerola.manga.repository.adapter.remote.mangadex.chapter

import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.remote.mangadex.api.MangadexMetadataChapterService
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterFileMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.manga.repository.port.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMetadataChapterService @Inject constructor(
    private val api: MangadexMetadataChapterService
) : ApiRepository.MetadataOperations<ChapterMetadataDto, String> {

    override suspend fun searchMetadata(
        manga: String, limit: Int, offset: Int, vararg extra: String?
    ): List<ChapterMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val responseFeed = api.getMangaFeed(mangaId = manga, limit = limit, offset = offset)
                val chaptersMetadataList = responseFeed.data

                val deferredChapters = chaptersMetadataList.map { metadataItem ->
                    async {
                        try {
                            val fileDto = api.getChapterImages(chapterId = metadataItem.id)

                            fromChapterData(
                                metadataDto = metadataItem,
                                fileDto = fileDto
                            )
                            // TODO: Tratar erro melhor
                        } catch (exception: Exception) {
                            fromChapterData(
                                metadataDto = metadataItem,
                                fileDto = null
                            )
                        }
                    }
                }

                deferredChapters.awaitAll()
            } catch (httpException: HttpException) {
                throw MangadexRequestException(
                    title = R.string.title_http_error,
                    description = if (httpException.code() == 429) R.string.description_http_error_rate_limit
                    else R.string.description_http_error_generic
                )
            } catch (exception: Exception) {
                // TODO: Tratar erro melhor
                println("Erro ao sincronizar $exception")
                throw MangadexRequestException(
                    title = R.string.title_metadata_request_error,
                    description = R.string.description_metadata_request_error
                )
            }
        }
    }

    private fun fromChapterDataList(dataList: List<ChapterMangadexDto>): List<ChapterMetadataDto> =
        dataList.map { fromChapterData(metadataDto = it, fileDto = null) }

    private fun fromChapterData(
        metadataDto: ChapterMangadexDto,
        fileDto: ChapterFileMangadexDto? = null
    ): ChapterMetadataDto {
        val attributes = metadataDto.attributes
        val scanlatorName = metadataDto.scanlationGroups
            .firstNotNullOfOrNull { it.attributes?.name }

        val pagesUrls = if (fileDto != null && fileDto.chapter.isNotEmpty()) {
            val dataSaver = fileDto.chapter.first()
            val baseUrl = fileDto.baseUrl
            val hash = dataSaver.hash

            dataSaver.data.map { fileName ->
                "$baseUrl/data/$hash/$fileName"
            }
        } else {
            emptyList()
        }

        return ChapterMetadataDto(
            id = metadataDto.id,
            volume = attributes.volume,
            chapter = attributes.chapter,
            title = attributes.title,
            scanlator = scanlatorName,
            pages = attributes.pages,
            pageUrls = pagesUrls
        )
    }
}