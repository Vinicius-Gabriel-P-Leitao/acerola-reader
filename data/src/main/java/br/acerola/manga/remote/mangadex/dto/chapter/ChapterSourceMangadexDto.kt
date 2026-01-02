package br.acerola.manga.remote.mangadex.dto.chapter

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChapterSourceMangadexDto(
    val baseUrl: String,
    val chapter: List<ChapterPage> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChapterPage(
    val hash: String,
    val data: List<String> = emptyList()
)
