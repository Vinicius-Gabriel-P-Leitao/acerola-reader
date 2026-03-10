package br.acerola.manga.local.mapper

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.local.database.entity.history.ReadingHistory
import br.acerola.manga.local.database.entity.history.ReadingHistoryWithChapter

fun ReadingHistory.toDto() = ReadingHistoryDto(
    mangaId = mangaId,
    chapterId = chapterId,
    lastPage = lastPage,
    updatedAt = updatedAt
)

fun ReadingHistoryDto.toEntity() = ReadingHistory(
    mangaId = mangaId,
    chapterId = chapterId,
    lastPage = lastPage,
    updatedAt = updatedAt
)

fun ReadingHistoryWithChapter.toDto() = ReadingHistoryWithChapterDto(
    mangaId = mangaId,
    chapterId = chapterId,
    lastPage = lastPage,
    updatedAt = updatedAt,
    chapterName = chapterName
)
