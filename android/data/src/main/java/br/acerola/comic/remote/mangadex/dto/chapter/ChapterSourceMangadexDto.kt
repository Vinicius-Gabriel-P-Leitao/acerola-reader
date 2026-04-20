package br.acerola.comic.remote.mangadex.dto.chapter

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChapterSourceMangadexDto(
    val baseUrl: String,
    val chapter: ChapterPage,
)

@JsonClass(generateAdapter = true)
data class ChapterPage(
    val hash: String,
    val data: List<String> = emptyList(),
)
