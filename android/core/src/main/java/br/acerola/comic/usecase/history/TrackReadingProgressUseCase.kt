package br.acerola.comic.usecase.history

import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.adapter.history.LocalHistory
import br.acerola.comic.dto.history.ReadingHistoryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackReadingProgressUseCase
    @Inject
    constructor(
        @param:LocalHistory private val historyRepository: HistoryGateway,
    ) {
        suspend fun markChapterAsRead(
            comicId: Long,
            chapterSort: String,
            chapterId: Long? = null,
        ) {
            historyRepository.markChapterAsRead(comicId, chapterSort, chapterId)
        }

        suspend fun toggleReadStatus(
            comicId: Long,
            chapterSort: String,
            isRead: Boolean,
            chapterId: Long? = null,
        ) {
            if (isRead) {
                historyRepository.unmarkChapterAsRead(comicId, chapterSort)
            } else {
                markChapterAsRead(comicId, chapterSort, chapterId)
            }
        }

        suspend fun saveProgress(history: ReadingHistoryDto) {
            historyRepository.upsertHistory(history)
        }
    }
