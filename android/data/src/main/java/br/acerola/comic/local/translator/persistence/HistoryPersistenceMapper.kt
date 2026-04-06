package br.acerola.comic.local.translator.persistence

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.local.entity.history.ReadingHistory

fun ReadingHistoryDto.toEntity() = ReadingHistory(
    mangaDirectoryId = mangaDirectoryId,
    chapterArchiveId = chapterArchiveId,
    lastPage = lastPage,
    isCompleted = isCompleted,
    updatedAt = updatedAt
)
