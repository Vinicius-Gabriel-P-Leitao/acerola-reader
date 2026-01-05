package br.acerola.manga.repository.adapter.remote.mangadex.manga

import android.content.Context
import arrow.core.Either
import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexMangaInfoApi
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto
import br.acerola.manga.repository.port.ApiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class MangadexMangaInfoService @Inject constructor(
    @param:ApplicationContext private val context: Context, private val api: MangadexMangaInfoApi
) : ApiRepository.RemoteInfoOperations<MangaRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, vararg extra: String?
    ): Either<NetworkError, List<MangaRemoteInfoDto>> = safeApiCall {
        withContext(context = Dispatchers.IO) {
            val response = api.searchMangaByName(title = manga, limit = limit, offset = offset)
            fromMangaDataList(dataList = response.data)
        }

    }

    private fun fromMangaData(mangaMangadexDto: MangaMangadexDto): MangaRemoteInfoDto {
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
                url = mangaMangadexDto.getCoverUrl() ?: "",
                fileName = mangaMangadexDto.coverFileName!!,
            )
        } else null

        val genresList: List<GenreDto> = attributes.tags.mapNotNull {
            val name = it.attributes.name

            if (!name.isNullOrBlank()) {
                GenreDto(
                    id = it.id, name = name
                )
            } else null
        }

        // NOTE: Sigla para tradução de romanji é default ja-ro
        val romanji: String? = attributes.altTitlesList.flatMap { it.entries }.find { it.key == "ja-ro" }?.value
            ?: attributes.titleMap["ja-ro"]

        // TODO: Tranformar em um toDto
        return MangaRemoteInfoDto(
            mirrorId = mangaMangadexDto.id,
            title = attributes.title ?: context.getString(R.string.description_manga_untitled),
            description = attributes.description ?: "",
            romanji = romanji,
            year = attributes.year,
            status = attributes.status,
            cover = coverDto,
            genre = genresList,
            authors = authors
        )
    }

    private fun fromMangaDataList(dataList: List<MangaMangadexDto>): List<MangaRemoteInfoDto> =
        dataList.map { fromMangaData(mangaMangadexDto = it) }
}