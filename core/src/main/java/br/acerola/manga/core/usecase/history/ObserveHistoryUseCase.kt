package br.acerola.manga.core.usecase.history

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.adapter.history.LocalHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveHistoryUseCase @Inject constructor(
    @param:LocalHistory private val historyRepository: HistoryGateway
) {
    operator fun invoke(): Flow<List<ReadingHistoryWithChapterDto>> =
        historyRepository.getAllRecentHistoryWithChapter()

    fun invokeRecent(): Flow<List<ReadingHistoryDto>> =
        historyRepository.getAllRecentHistory()
}
