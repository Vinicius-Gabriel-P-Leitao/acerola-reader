package br.acerola.manga.repository.adapter.remote.mangadex.manga

import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.remote.mangadex.api.MangadexMetadataMangaService
import br.acerola.manga.remote.mangadex.dto.MangaDexResponse
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto
import br.acerola.manga.repository.port.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMetadataMangaService @Inject constructor(
    private val api: MangadexMetadataMangaService
) : ApiRepository.MetadataOperations<MangaMetadataDto, String> {
    override suspend fun searchMetadata(
        manga: String, limit: Int, offset: Int, vararg extra: String?
    ): List<MangaMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val response: MangaDexResponse<MangaMangadexDto> = api.searchMangaByName(manga, limit, offset)
                fromMangaDataList(dataList = response.data)
            } catch (httpException: HttpException) {
                throw MangadexRequestException(
                    title = R.string.title_http_error,
                    description = if (httpException.code() == 429) R.string.description_http_error_rate_limit
                    else R.string.description_http_error_generic
                )
            } catch (exception: Exception) {
                println("Erro ao sincronizar $exception")
                throw MangadexRequestException(
                    title = R.string.title_metadata_request_error,
                    description = R.string.description_metadata_request_error
                )
            }
        }
    }

    private fun fromMangaDataList(dataList: List<MangaMangadexDto>): List<MangaMetadataDto> =
        dataList.map { fromMangaData(mangaMangadexDto = it) }

    private fun fromMangaData(mangaMangadexDto: MangaMangadexDto): MangaMetadataDto {
        val attributes = mangaMangadexDto.attributes

        val authors = if (mangaMangadexDto.authorName != null && mangaMangadexDto.authorId != null) {
            AuthorDto(
                id = mangaMangadexDto.authorId!!,
                name = mangaMangadexDto.authorName!!,
                type = mangaMangadexDto.authorType!!
            )
        } else null

        val coverDto = if (mangaMangadexDto.coverFileName != null && mangaMangadexDto.coverId != null) {
            CoverDto(
                id = mangaMangadexDto.coverId!!,
                fileName = mangaMangadexDto.coverFileName!!,
                url = mangaMangadexDto.getCoverUrl() ?: ""
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
            id = mangaMangadexDto.id,
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