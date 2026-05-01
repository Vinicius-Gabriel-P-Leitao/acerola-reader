package br.acerola.comic.local.translator.ui

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto
import br.acerola.comic.local.entity.history.ReadingHistory
import br.acerola.comic.local.entity.relation.ReadingHistoryWithChapter

fun ReadingHistory.toViewDto() =
    ReadingHistoryDto(
        comicDirectoryId = comicDirectoryId,
        chapterArchiveId = chapterArchiveId,
        chapterSort = chapterSort,
        lastPage = lastPage,
        isCompleted = isCompleted,
        updatedAt = updatedAt,
    )

fun ReadingHistoryWithChapter.toViewDto() =
    ReadingHistoryWithChapterDto(
        comicDirectoryId = comicDirectoryId,
        chapterArchiveId = chapterArchiveId,
        chapterSort = chapterSort,
        lastPage = lastPage,
        updatedAt = updatedAt,
        chapterName = chapterName,
        isCompleted = isCompleted,
    )
