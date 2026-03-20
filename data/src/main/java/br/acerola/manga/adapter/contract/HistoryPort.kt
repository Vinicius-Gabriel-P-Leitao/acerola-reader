package br.acerola.manga.adapter.contract

import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import kotlinx.coroutines.flow.Flow

interface HistoryPort {
    fun getHistoryByMangaId(mangaId: Long): Flow<ReadingHistoryDto?>
    fun getAllRecentHistory(): Flow<List<ReadingHistoryDto>>
    fun getAllRecentHistoryWithChapter(): Flow<List<ReadingHistoryWithChapterDto>>
    fun getReadChaptersByMangaId(mangaId: Long): Flow<List<Long>>
    suspend fun upsertHistory(history: ReadingHistoryDto)
    suspend fun markChapterAsRead(mangaId: Long, chapterId: Long)
    suspend fun unmarkChapterAsRead(chapterId: Long)
    suspend fun deleteHistory(mangaId: Long)
}
