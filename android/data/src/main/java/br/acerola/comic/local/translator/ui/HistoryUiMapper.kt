package br.acerola.comic.local.translator.ui

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto
import br.acerola.comic.local.entity.history.ReadingHistory
import br.acerola.comic.local.entity.relation.ReadingHistoryWithChapter

fun ReadingHistory.toViewDto() = ReadingHistoryDto(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    isCompleted = isCompleted,
    updatedAt = updatedAt
)

fun ReadingHistoryWithChapter.toViewDto() = ReadingHistoryWithChapterDto(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    updatedAt = updatedAt,
    chapterName = chapterName,
    isCompleted = isCompleted
)
