package br.acerola.manga.local.database.entity.history

data class ReadingHistoryWithChapter(
    val mangaId: Long,
    val chapterId: Long,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterName: String?
)
