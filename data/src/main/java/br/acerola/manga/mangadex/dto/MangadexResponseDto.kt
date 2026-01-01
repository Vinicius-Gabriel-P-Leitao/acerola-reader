package br.acerola.manga.mangadex.dto

data class MangaDexResponse<T>(
    val result: String,
    val response: String,
    val data: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Int
)

