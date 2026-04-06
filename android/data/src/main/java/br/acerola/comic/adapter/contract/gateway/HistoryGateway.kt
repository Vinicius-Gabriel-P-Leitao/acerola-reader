package br.acerola.comic.adapter.contract.gateway

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto
import kotlinx.coroutines.flow.Flow

interface HistoryGateway {
    fun getHistoryByMangaId(mangaId: Long): Flow<ReadingHistoryDto?>
    fun getAllRecentHistory(): Flow<List<ReadingHistoryDto>>
    fun getAllRecentHistoryWithChapter(): Flow<List<ReadingHistoryWithChapterDto>>
    fun getReadChaptersByMangaId(mangaId: Long): Flow<List<Long>>
    suspend fun upsertHistory(history: ReadingHistoryDto)
    suspend fun markChapterAsRead(mangaId: Long, chapterId: Long)
    suspend fun unmarkChapterAsRead(chapterId: Long)
    suspend fun deleteHistory(mangaId: Long)
}
