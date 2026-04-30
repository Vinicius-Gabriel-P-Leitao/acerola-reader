package br.acerola.comic.local.entity.relation

// TODO: Pensar em nome melhor
data class ReadingHistoryWithChapter(
    val comicDirectoryId: Long,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterArchiveId: Long?,
    val chapterSort: String,
    val chapterName: String?,
    val isCompleted: Boolean,
)
