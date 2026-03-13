package br.acerola.manga.local.mapper

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.local.database.entity.history.ReadingHistory
import br.acerola.manga.local.database.entity.relation.ReadingHistoryWithChapter

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
