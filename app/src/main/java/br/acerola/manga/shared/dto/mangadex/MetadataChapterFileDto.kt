package br.acerola.manga.shared.dto.mangadex

data class MetadataChapterFileDto(
    val baseUrl: String,
    val chapter: List<ChapterPage> = emptyList()
)

data class ChapterPage(
    val hash: String,
    val data: List<String> = emptyList()
)
