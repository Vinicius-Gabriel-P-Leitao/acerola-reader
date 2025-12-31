package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.R
import br.acerola.manga.domain.builder.ChapterMetadataBuilder
import br.acerola.manga.domain.data.dao.api.mangadex.MangadexMetadataChapterDao
import br.acerola.manga.domain.service.api.ApiPort
import br.acerola.manga.shared.dto.metadata.ChapterMetadataDto
import br.acerola.manga.shared.error.exception.MangadexRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchChapterDataService @Inject constructor(
    private val api: MangadexMetadataChapterDao
) : ApiPort.MetadataOperations<ChapterMetadataDto, String> {

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

                            ChapterMetadataBuilder.fromChapterData(
                                metadataDto = metadataItem,
                                fileDto = fileDto
                            )
                            // TODO: Tratar erro melhor
                        } catch (exception: Exception) {
                            ChapterMetadataBuilder.fromChapterData(
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
                throw MangadexRequestException(
                    title = R.string.title_metadata_request_error,
                    description = R.string.description_metadata_request_error
                )
            }
        }
    }
}