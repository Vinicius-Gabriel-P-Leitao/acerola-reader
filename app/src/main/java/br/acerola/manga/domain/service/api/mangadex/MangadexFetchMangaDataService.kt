package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.R
import br.acerola.manga.domain.builder.MangaMetadataBuilder
import br.acerola.manga.domain.data.dao.api.mangadex.manga.MangaDataMangaDexDao
import br.acerola.manga.domain.service.api.ApiPort
import br.acerola.manga.shared.dto.mangadex.MetadataMangaDto
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.MangadexRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchMangaDataService @Inject constructor(
    private val api: MangaDataMangaDexDao
) : ApiPort.MetadataOperations<MangaMetadataDto, String> {
    override suspend fun searchManga(
        title: String, limit: Int, offset: Int, vararg extra: String?
    ): List<MangaMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val response: MangaDexResponse<MetadataMangaDto> = api.searchMangaByName(title, limit, offset)
                MangaMetadataBuilder.fromMangaDataList(dataList = response.data)
            } catch (httpException: HttpException) {
                throw MangadexRequestException(
                    title = R.string.title_http_error,
                    description = if (httpException.code() == 429) R.string.description_http_error_rate_limit
                    else R.string.description_http_error_generic
                )
            } catch (e: Exception) {
                throw MangadexRequestException(
                    title = R.string.title_metadata_request_error,
                    description = R.string.description_metadata_request_error
                )
            }
        }
    }
}
