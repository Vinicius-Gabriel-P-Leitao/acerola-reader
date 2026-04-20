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
            mangaId: Long,
            chapterId: Long,
        ) {
            historyRepository.markChapterAsRead(mangaId, chapterId)
        }

        suspend fun toggleReadStatus(
            mangaId: Long,
            chapterId: Long,
            isRead: Boolean,
        ) {
            if (isRead) {
                historyRepository.unmarkChapterAsRead(chapterId)
            } else {
                markChapterAsRead(mangaId, chapterId)
            }
        }

        suspend fun saveProgress(history: ReadingHistoryDto) {
            historyRepository.upsertHistory(history)
        }
    }
