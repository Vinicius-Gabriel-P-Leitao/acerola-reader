package br.acerola.comic.adapter.history

import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto
import br.acerola.comic.local.dao.history.ReadingHistoryDao
import br.acerola.comic.local.entity.history.ChapterRead
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHistoryEngine
    @Inject
    constructor(
        private val readingHistoryDao: ReadingHistoryDao,
    ) : HistoryGateway {
        override fun getHistoryByMangaId(mangaId: Long): Flow<ReadingHistoryDto?> =
            readingHistoryDao.observeHistoryByDirectoryId(mangaId).map { it?.toViewDto() }

        override fun getAllRecentHistory(): Flow<List<ReadingHistoryDto>> =
            readingHistoryDao.observeAllRecentHistories().map { list ->
                list.map {
                    it.toViewDto()
                }
            }

        override fun getAllRecentHistoryWithChapter(): Flow<List<ReadingHistoryWithChapterDto>> =
            readingHistoryDao.observeAllRecentHistoriesWithChapter().map { list ->
                list.map {
                    it.toViewDto()
                }
            }

        override fun getReadChaptersByMangaId(mangaId: Long): Flow<List<Long>> = readingHistoryDao.observeReadChaptersByDirectoryId(mangaId)

        override suspend fun upsertHistory(history: ReadingHistoryDto) {
            AcerolaLogger.d(TAG, "Updating history for mangaId: ${history.mangaDirectoryId}", LogSource.REPOSITORY)
            readingHistoryDao.upsertHistory(history.toEntity())
        }

        override suspend fun markChapterAsRead(
            mangaId: Long,
            chapterId: Long,
        ) {
            AcerolaLogger.d(TAG, "Marking chapter $chapterId as read for comic $mangaId", LogSource.REPOSITORY)
            readingHistoryDao.upsertChapterRead(ChapterRead(mangaDirectoryId = mangaId, chapterArchiveId = chapterId))
        }

        override suspend fun unmarkChapterAsRead(chapterId: Long) {
            AcerolaLogger.d(TAG, "Unmarking chapter $chapterId as read", LogSource.REPOSITORY)
            readingHistoryDao.deleteChapterRead(chapterId)
        }

        override suspend fun deleteHistory(mangaId: Long) {
            AcerolaLogger.audit(TAG, "User deleting reading reading history for comic: $mangaId", LogSource.REPOSITORY)
            readingHistoryDao.deleteHistoryByDirectoryId(mangaId)
        }

        companion object {
            private const val TAG = "LocalHistoryRepository"
        }
    }
