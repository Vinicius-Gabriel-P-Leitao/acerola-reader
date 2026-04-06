package br.acerola.comic.usecase.history

import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.adapter.history.LocalHistory
import br.acerola.comic.dto.history.ReadingHistoryDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveComicHistoryUseCase @Inject constructor(
    @param:LocalHistory private val historyRepository: HistoryGateway
) {
    fun observeByManga(mangaId: Long): Flow<ReadingHistoryDto?> =
        historyRepository.getHistoryByMangaId(mangaId)

    fun observeReadChapters(mangaId: Long): Flow<List<Long>> =
        historyRepository.getReadChaptersByMangaId(mangaId)
}
