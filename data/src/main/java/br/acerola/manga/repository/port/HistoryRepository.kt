package br.acerola.manga.repository.port

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import kotlinx.coroutines.flow.Flow

interface HistoryManagementRepository {
    fun getHistoryByMangaId(mangaId: Long): Flow<ReadingHistoryDto?>
    fun getAllRecentHistory(): Flow<List<ReadingHistoryDto>>
    fun getAllRecentHistoryWithChapter(): Flow<List<ReadingHistoryWithChapterDto>>
    suspend fun upsertHistory(history: ReadingHistoryDto)
    suspend fun deleteHistory(mangaId: Long)
}
