package br.acerola.manga.usecase.history

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.repository.port.HistoryManagementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryManagementRepository
) {
    operator fun invoke(): Flow<List<ReadingHistoryWithChapterDto>> =
        historyRepository.getAllRecentHistoryWithChapter()

    fun invokeRecent(): Flow<List<ReadingHistoryDto>> =
        historyRepository.getAllRecentHistory()
}
