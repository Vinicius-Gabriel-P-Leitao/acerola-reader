package br.acerola.manga.local.translator

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.local.entity.history.ReadingHistory
import br.acerola.manga.local.entity.relation.ReadingHistoryWithChapter

fun ReadingHistory.toDto() = ReadingHistoryDto(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    isCompleted = isCompleted,
    updatedAt = updatedAt
)

fun ReadingHistoryDto.toEntity() = ReadingHistory(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    isCompleted = isCompleted,
    updatedAt = updatedAt
)

fun ReadingHistoryWithChapter.toDto() = ReadingHistoryWithChapterDto(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    updatedAt = updatedAt,
    chapterName = chapterName,
    isCompleted = isCompleted
)
