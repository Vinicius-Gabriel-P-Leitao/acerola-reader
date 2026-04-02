package br.acerola.manga.remote.mangadex.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaDexEntityResponseDto<T>(
    val result: String,
    val response: String,
    val data: T
)
