package br.acerola.manga.mangadex.dto.chapter

data class ChapterFileMangadexDto(
    val baseUrl: String,
    val chapter: List<ChapterPage> = emptyList()
)

data class ChapterPage(
    val hash: String,
    val data: List<String> = emptyList()
)
