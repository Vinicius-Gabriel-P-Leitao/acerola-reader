package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.R
import br.acerola.manga.domain.data.dao.api.mangadex.MangadexMetadataMangaDao
import br.acerola.manga.domain.service.api.ApiPort
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.mangadex.MetadataMangaDto
import br.acerola.manga.shared.dto.metadata.AuthorDto
import br.acerola.manga.shared.dto.metadata.CoverDto
import br.acerola.manga.shared.dto.metadata.GenreDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.MangadexRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchMangaDataService @Inject constructor(
    private val api: MangadexMetadataMangaDao
) : ApiPort.MetadataOperations<MangaMetadataDto, String> {
    override suspend fun searchMetadata(
        manga: String, limit: Int, offset: Int, vararg extra: String?
    ): List<MangaMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val response: MangaDexResponse<MetadataMangaDto> = api.searchMangaByName(manga, limit, offset)
                fromMangaDataList(dataList = response.data)
            } catch (httpException: HttpException) {
                throw MangadexRequestException(
                    title = R.string.title_http_error,
                    description = if (httpException.code() == 429) R.string.description_http_error_rate_limit
                    else R.string.description_http_error_generic
                )
            } catch (exception: Exception) {
                throw MangadexRequestException(
                    title = R.string.title_metadata_request_error,
                    description = R.string.description_metadata_request_error
                )
            }
        }
    }

    private fun fromMangaDataList(dataList: List<MetadataMangaDto>): List<MangaMetadataDto> =
        dataList.map { fromMangaData(metadataMangaDto = it) }

    private fun fromMangaData(metadataMangaDto: MetadataMangaDto): MangaMetadataDto {
        val attributes = metadataMangaDto.attributes

        val authors = if (metadataMangaDto.authorName != null && metadataMangaDto.authorId != null) {
            AuthorDto(
                id = metadataMangaDto.authorId!!,
                name = metadataMangaDto.authorName!!,
                type = metadataMangaDto.authorType!!
            )
        } else null

        val coverDto = if (metadataMangaDto.coverFileName != null && metadataMangaDto.coverId != null) {
            CoverDto(
                id = metadataMangaDto.coverId!!,
                fileName = metadataMangaDto.coverFileName!!,
                url = metadataMangaDto.getCoverUrl() ?: ""
            )
        } else null

        val genresList: List<GenreDto> = attributes.tags.mapNotNull { tag ->
            val name = tag.attributes.name
            if (!name.isNullOrBlank()) {
                GenreDto(id = tag.id, name = name)
            } else null
        }

        val romanji: String? = attributes.altTitlesList
            .flatMap { it.entries }
            .find { it.key == "ja-ro" }?.value
            ?: attributes.titleMap["ja-ro"]

        // TODO: String para valores default
        return MangaMetadataDto(
            id = metadataMangaDto.id,
            title = attributes.title ?: "Sem Título",
            description = attributes.description ?: "",
            romanji = romanji,
            year = attributes.year,
            status = attributes.status,
            cover = coverDto,
            gender = genresList,
            authors = authors
        )
    }


}
