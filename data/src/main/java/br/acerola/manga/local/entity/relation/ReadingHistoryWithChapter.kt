package br.acerola.manga.local.entity.relation

data class ReadingHistoryWithChapter(
    val mangaDirectoryId: Long,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterArchiveId: Long,
    val chapterName: String?,
    val isCompleted: Boolean
)