package br.acerola.comic.local.dao.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.comic.local.entity.history.ChapterRead
import br.acerola.comic.local.entity.history.ReadingHistory
import br.acerola.comic.local.entity.relation.ReadingHistoryWithChapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: ReadingHistory)

    @Query("SELECT * FROM reading_history WHERE comic_directory_id = :mangaId")
    fun observeHistoryByDirectoryId(mangaId: Long): Flow<ReadingHistory?>

    @Query("SELECT * FROM reading_history ORDER BY updated_at DESC")
    fun observeAllRecentHistories(): Flow<List<ReadingHistory>>

    @Query(
        """
        SELECT rh.comic_directory_id as mangaDirectoryId, rh.chapter_archive_id as chapterArchiveId, rh.last_page as lastPage, rh.updated_at as updatedAt, ca.chapter as chapterName, rh.is_completed as isCompleted
        FROM reading_history rh
        LEFT JOIN chapter_archive ca ON rh.chapter_archive_id = ca.id
        ORDER BY rh.updated_at DESC
    """,
    )
    fun observeAllRecentHistoriesWithChapter(): Flow<List<ReadingHistoryWithChapter>>

    @Query("DELETE FROM reading_history WHERE comic_directory_id = :mangaId")
    suspend fun deleteHistoryByDirectoryId(mangaId: Long)

    // Chapter Read
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapterRead(chapterRead: ChapterRead)

    @Query("SELECT chapter_archive_id FROM chapter_read WHERE comic_directory_id = :mangaId")
    fun observeReadChaptersByDirectoryId(mangaId: Long): Flow<List<Long>>

    @Query("DELETE FROM chapter_read WHERE chapter_archive_id = :chapterId")
    suspend fun deleteChapterRead(chapterId: Long)
}
