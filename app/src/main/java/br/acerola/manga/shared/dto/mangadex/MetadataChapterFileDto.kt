package br.acerola.manga.shared.dto.mangadex

data class MetadataChapterFileDto(
    val baseUrl: String,
    val chapter: List<ChapterDataSaver> = emptyList()
)

data class ChapterDataSaver(
    val hash: String,
    val data: List<String> = emptyList()
)
