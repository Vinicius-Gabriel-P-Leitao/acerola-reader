package br.acerola.comic.usecase.history

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto
import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.adapter.history.LocalHistory
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
