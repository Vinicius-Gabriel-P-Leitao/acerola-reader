package br.acerola.manga.remote.mangadex.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaDexResponse<T>(
    val result: String,
    val response: String,
    val data: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Int
)

