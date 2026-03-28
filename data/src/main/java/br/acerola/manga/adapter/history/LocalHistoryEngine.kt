package br.acerola.manga.adapter.history

import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.local.dao.history.ReadingHistoryDao
import br.acerola.manga.local.entity.history.ChapterRead
import br.acerola.manga.local.translator.persistence.toEntity
import br.acerola.manga.local.translator.ui.toViewDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHistoryEngine @Inject constructor(
    private val readingHistoryDao: ReadingHistoryDao
) : HistoryGateway {

    override fun getHistoryByMangaId(mangaId: Long): Flow<ReadingHistoryDto?> {
        return readingHistoryDao.getByMangaId(mangaId).map { it?.toViewDto() }
    }

    override fun getAllRecentHistory(): Flow<List<ReadingHistoryDto>> {
        return readingHistoryDao.getAllRecent().map { list -> list.map { it.toViewDto() } }
    }

    override fun getAllRecentHistoryWithChapter(): Flow<List<ReadingHistoryWithChapterDto>> {
        return readingHistoryDao.getAllRecentWithChapterName().map { list -> list.map { it.toViewDto() } }
    }

    override fun getReadChaptersByMangaId(mangaId: Long): Flow<List<Long>> {
        return readingHistoryDao.getReadChaptersByMangaId(mangaId)
    }

    override suspend fun upsertHistory(history: ReadingHistoryDto) {
        AcerolaLogger.d(TAG, "Updating history for mangaId: ${history.mangaDirectoryId}", LogSource.REPOSITORY)
        readingHistoryDao.upsert(history.toEntity())
    }

    override suspend fun markChapterAsRead(mangaId: Long, chapterId: Long) {
        AcerolaLogger.d(TAG, "Marking chapter $chapterId as read for manga $mangaId", LogSource.REPOSITORY)
        readingHistoryDao.markChapterAsRead(ChapterRead(mangaDirectoryId = mangaId, chapterArchiveId = chapterId))
    }

    override suspend fun unmarkChapterAsRead(chapterId: Long) {
        AcerolaLogger.d(TAG, "Unmarking chapter $chapterId as read", LogSource.REPOSITORY)
        readingHistoryDao.unmarkChapterAsRead(chapterId)
    }

    override suspend fun deleteHistory(mangaId: Long) {
        AcerolaLogger.audit(TAG, "User deleting reading reading history for manga: $mangaId", LogSource.REPOSITORY)
        readingHistoryDao.deleteByMangaId(mangaId)
    }

    companion object {
        private const val TAG = "LocalHistoryRepository"
    }
}
