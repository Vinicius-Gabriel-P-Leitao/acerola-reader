package br.acerola.manga.data.repository

import br.acerola.manga.local.database.dao.history.ReadingHistoryDao
import br.acerola.manga.local.database.entity.history.ReadingHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val readingHistoryDao: ReadingHistoryDao
) {
    fun getHistoryByMangaId(mangaId: Long): Flow<ReadingHistory?> {
        return readingHistoryDao.getByMangaId(mangaId)
    }

    fun getAllRecentHistory(): Flow<List<ReadingHistory>> {
        return readingHistoryDao.getAllRecent()
    }

    suspend fun upsertHistory(history: ReadingHistory) {
        readingHistoryDao.upsert(history)
    }

    suspend fun deleteHistory(mangaId: Long) {
        readingHistoryDao.deleteByMangaId(mangaId)
    }
}
