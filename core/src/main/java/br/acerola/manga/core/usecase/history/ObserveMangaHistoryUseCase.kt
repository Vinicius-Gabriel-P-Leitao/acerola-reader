package br.acerola.manga.core.usecase.history

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.adapter.di.LocalHistoryEngine
import br.acerola.manga.adapter.port.HistoryPort
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveMangaHistoryUseCase @Inject constructor(
    @param:LocalHistoryEngine private val historyRepository: HistoryPort
) {
    fun observeByManga(mangaId: Long): Flow<ReadingHistoryDto?> =
        historyRepository.getHistoryByMangaId(mangaId)

    fun observeReadChapters(mangaId: Long): Flow<List<Long>> =
        historyRepository.getReadChaptersByMangaId(mangaId)

    suspend fun toggleReadStatus(mangaId: Long, chapterId: Long, isRead: Boolean) {
        if (isRead) {
            historyRepository.unmarkChapterAsRead(chapterId)
        } else {
            historyRepository.markChapterAsRead(mangaId, chapterId)
        }
    }
}
