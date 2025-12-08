package br.acerola.manga.domain.data.dao.api.mangadex.cover

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MangaDexDownloadDao {
    @GET
    @Streaming
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody
}