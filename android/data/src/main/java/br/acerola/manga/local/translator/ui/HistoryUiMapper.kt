package br.acerola.manga.local.translator.ui

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.local.entity.history.ReadingHistory
import br.acerola.manga.local.entity.relation.ReadingHistoryWithChapter

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
