package br.acerola.comic.remote.mangadex.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangadexResponseDto<T>(
    val result: String,
    val response: String,
    val data: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)
