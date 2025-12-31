package br.acerola.manga.domain.data.dao.api.mangadex

import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.mangadex.MetadataChapterDto
import br.acerola.manga.shared.dto.mangadex.MetadataChapterFileDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangadexMetadataChapterDao {
    @GET(value = "manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path(value = "id") mangaId: String,
        @Query(value = "translatedLanguage[]") languages: List<String> = listOf("pt-br"),
        @Query(value = "order[createdAt]") order: String = "asc",
        @Query(value = "includes[]") includes: List<String> = listOf("scanlation_group"),
        @Query(value = "limit") limit: Int = 100,
        @Query(value = "offset") offset: Int = 0
    ): MangaDexResponse<MetadataChapterDto>

    @GET(value = "at-home/server/{chapterId}")
    suspend fun getChapterImages(
        @Path(value = "chapterId") chapterId: String
    ): MetadataChapterFileDto

    @GET
    @retrofit2.http.Streaming
    suspend fun downloadChapterPage(
        @retrofit2.http.Url fullUrl: String
    ): okhttp3.ResponseBody
}