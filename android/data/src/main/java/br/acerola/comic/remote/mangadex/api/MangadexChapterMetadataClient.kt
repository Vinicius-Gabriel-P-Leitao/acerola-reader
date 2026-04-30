package br.acerola.comic.remote.mangadex.api

import br.acerola.comic.remote.mangadex.dto.MangadexResponseDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MangadexChapterMetadataClient {
    @GET(value = "comic/{id}/feed")
    suspend fun getMangaFeed(
        @Path(value = "id") comicId: String,
        @Query(value = "translatedLanguage[]") languages: List<String>,
        @Query(value = "includes[]") includes: List<String> = listOf("scanlation_group"),
        @Query(value = "order[chapter]") order: String = "asc",
        @Query(value = "limit") limit: Int = 100,
        @Query(value = "offset") offset: Int = 0,
    ): MangadexResponseDto<ChapterMangadexDto>

    @GET(value = "at-home/server/{chapterId}")
    suspend fun getChapterImages(
        @Path(value = "chapterId") chapterId: String,
    ): ChapterSourceMangadexDto

    @GET
    @Streaming
    suspend fun downloadChapterPage(
        @Url fullUrl: String,
    ): ResponseBody
}
