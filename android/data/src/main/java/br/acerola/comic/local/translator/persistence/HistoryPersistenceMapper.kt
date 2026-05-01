package br.acerola.comic.local.translator.persistence

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.local.entity.history.ReadingHistory

fun ReadingHistoryDto.toEntity() =
    ReadingHistory(
        comicDirectoryId = comicDirectoryId,
        chapterArchiveId = chapterArchiveId,
        chapterSort = chapterSort,
        lastPage = lastPage,
        isCompleted = isCompleted,
        updatedAt = updatedAt,
    )
