package br.acerola.manga.core.usecase.history

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.adapter.di.LocalHistoryEngine
import br.acerola.manga.adapter.port.HistoryPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackReadingProgressUseCase @Inject constructor(
    @param:LocalHistoryEngine private val historyRepository: HistoryPort
) {
    suspend fun saveProgress(mangaId: Long, chapterId: Long, page: Int, markAsRead: Boolean) {
        historyRepository.upsertHistory(
            ReadingHistoryDto(
                mangaDirectoryId = mangaId,
                chapterArchiveId = chapterId,
                lastPage = page,
                isCompleted = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (markAsRead) {
            historyRepository.markChapterAsRead(mangaId, chapterId)
        }
    }

    suspend fun markAsRead(mangaId: Long, chapterId: Long) {
        historyRepository.markChapterAsRead(mangaId, chapterId)
    }
}
