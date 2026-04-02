package br.acerola.manga.core.usecase.history

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.adapter.history.LocalHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveMangaHistoryUseCase @Inject constructor(
    @param:LocalHistory private val historyRepository: HistoryGateway
) {
    fun observeByManga(mangaId: Long): Flow<ReadingHistoryDto?> =
        historyRepository.getHistoryByMangaId(mangaId)

    fun observeReadChapters(mangaId: Long): Flow<List<Long>> =
        historyRepository.getReadChaptersByMangaId(mangaId)
}
