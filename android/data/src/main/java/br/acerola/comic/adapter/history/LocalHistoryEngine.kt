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
        override fun getHistoryByMangaId(comicId: Long): Flow<ReadingHistoryDto?> =
            readingHistoryDao.observeHistoryByDirectoryId(comicId).map { it?.toViewDto() }

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

        override fun getReadChaptersByMangaId(comicId: Long): Flow<List<String>> = readingHistoryDao.observeReadChaptersByDirectoryId(comicId)

        override suspend fun upsertHistory(history: ReadingHistoryDto) {
            AcerolaLogger.d(TAG, "Updating history for comicId: ${history.comicDirectoryId}", LogSource.REPOSITORY)
            readingHistoryDao.upsertHistory(history.toEntity())
        }

        override suspend fun markChapterAsRead(
            comicId: Long,
            chapterSort: String,
            chapterId: Long?,
        ) {
            AcerolaLogger.d(TAG, "Marking chapter $chapterSort (ID: $chapterId) as read for comic $comicId", LogSource.REPOSITORY)
            readingHistoryDao.upsertChapterRead(
                ChapterRead(
                    comicDirectoryId = comicId,
                    chapterSort = chapterSort,
                    chapterArchiveId = chapterId,
                ),
            )
        }

        override suspend fun unmarkChapterAsRead(
            comicId: Long,
            chapterSort: String,
        ) {
            AcerolaLogger.d(TAG, "Unmarking chapter $chapterSort as read for comic $comicId", LogSource.REPOSITORY)
            readingHistoryDao.deleteChapterRead(comicId, chapterSort)
        }

        override suspend fun updateChapterIdBySort(
            comicId: Long,
            chapterSort: String,
            newId: Long,
        ) {
            readingHistoryDao.updateHistoryChapterIdBySort(comicId, chapterSort, newId)
            readingHistoryDao.updateChapterReadIdBySort(comicId, chapterSort, newId)
        }

        override suspend fun deleteHistory(comicId: Long) {
            AcerolaLogger.audit(TAG, "User deleting reading reading history for comic: $comicId", LogSource.REPOSITORY)
            readingHistoryDao.deleteHistoryByDirectoryId(comicId)
        }

        companion object {
            private const val TAG = "LocalHistoryRepository"
        }
    }
