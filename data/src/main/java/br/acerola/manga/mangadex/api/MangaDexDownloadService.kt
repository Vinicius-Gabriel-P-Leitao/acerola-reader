package br.acerola.manga.mangadex.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MangaDexDownloadService {
    @GET
    @Streaming
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody
}