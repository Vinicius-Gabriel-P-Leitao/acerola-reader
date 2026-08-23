package br.acerola.comic.remote.mangadex.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MangadexMangaDownloadClient {
    @GET
    @Streaming
    suspend fun downloadFile(
        @Url fileUrl: String,
    ): ResponseBody
}
