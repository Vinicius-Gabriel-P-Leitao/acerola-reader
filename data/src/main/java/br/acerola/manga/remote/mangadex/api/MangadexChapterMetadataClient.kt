package br.acerola.manga.remote.mangadex.api

import br.acerola.manga.remote.mangadex.dto.MangaDexResponse
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MangadexChapterMetadataClient {
    @GET(value = "manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path(value = "id") mangaId: String,
        // TODO: Adicionar essa seleceção de idiomas no config global.
        @Query(value = "translatedLanguage[]") languages: List<String> = listOf("pt-br"),
        @Query(value = "order[chapter]") order: String = "asc",
        @Query(value = "includes[]") includes: List<String> = listOf("scanlation_group"),
        @Query(value = "limit") limit: Int = 100,
        @Query(value = "offset") offset: Int = 0
    ): MangaDexResponse<ChapterMangadexDto>

    @GET(value = "at-home/server/{chapterId}")
    suspend fun getChapterImages(
        @Path(value = "chapterId") chapterId: String
    ): ChapterSourceMangadexDto

    @GET
    @Streaming
    suspend fun downloadChapterPage(
        @Url fullUrl: String
    ): ResponseBody
}