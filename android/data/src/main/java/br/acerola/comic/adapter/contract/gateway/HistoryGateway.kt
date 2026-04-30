package br.acerola.comic.adapter.contract.gateway

import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto
import kotlinx.coroutines.flow.Flow

interface HistoryGateway {
    fun getHistoryByMangaId(comicId: Long): Flow<ReadingHistoryDto?>

    fun getAllRecentHistory(): Flow<List<ReadingHistoryDto>>

    fun getAllRecentHistoryWithChapter(): Flow<List<ReadingHistoryWithChapterDto>>

    fun getReadChaptersByMangaId(comicId: Long): Flow<List<String>>

    suspend fun upsertHistory(history: ReadingHistoryDto)

    suspend fun markChapterAsRead(
        comicId: Long,
        chapterSort: String,
        chapterId: Long? = null,
    )

    suspend fun unmarkChapterAsRead(
        comicId: Long,
        chapterSort: String,
    )

    suspend fun deleteHistory(comicId: Long)
}
