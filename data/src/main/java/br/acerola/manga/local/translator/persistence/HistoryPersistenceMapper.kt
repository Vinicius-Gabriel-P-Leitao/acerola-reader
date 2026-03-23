package br.acerola.manga.local.translator.persistence

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.local.entity.history.ReadingHistory

fun ReadingHistoryDto.toEntity() = ReadingHistory(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    isCompleted = isCompleted,
    updatedAt = updatedAt
)
