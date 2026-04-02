package br.acerola.manga.core.usecase.history

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.adapter.history.LocalHistory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackReadingProgressUseCase @Inject constructor(
    @param:LocalHistory private val historyRepository: HistoryGateway
) {
    suspend fun markChapterAsRead(mangaId: Long, chapterId: Long) {
        historyRepository.markChapterAsRead(mangaId, chapterId)
    }

    suspend fun unmarkChapterAsRead(chapterId: Long) {
        historyRepository.unmarkChapterAsRead(chapterId)
    }

    suspend fun toggleReadStatus(mangaId: Long, chapterId: Long, isRead: Boolean) {
        if (isRead) {
            unmarkChapterAsRead(chapterId)
        } else {
            markChapterAsRead(mangaId, chapterId)
        }
    }

    suspend fun saveProgress(history: ReadingHistoryDto) {
        historyRepository.upsertHistory(history)
    }
}
