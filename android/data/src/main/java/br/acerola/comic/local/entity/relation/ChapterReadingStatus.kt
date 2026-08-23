package br.acerola.comic.local.entity.relation

data class ChapterReadingStatus(
    val comicDirectoryId: Long,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterArchiveId: Long?,
    val chapterSort: String,
    val chapterName: String?,
    val isCompleted: Boolean,
)
