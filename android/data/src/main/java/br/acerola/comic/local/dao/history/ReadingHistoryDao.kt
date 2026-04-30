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

    @Query("SELECT * FROM reading_history WHERE comic_directory_id = :comicId")
    fun observeHistoryByDirectoryId(comicId: Long): Flow<ReadingHistory?>

    @Query("SELECT * FROM reading_history ORDER BY updated_at DESC")
    fun observeAllRecentHistories(): Flow<List<ReadingHistory>>

    @Query(
        """
        SELECT
            rh.comic_directory_id AS comicDirectoryId,
            rh.chapter_archive_id AS chapterArchiveId,
            rh.chapter_sort AS chapterSort,
            rh.last_page AS lastPage,
            rh.updated_at AS updatedAt,
            ca.chapter AS chapterName,
            rh.is_completed AS isCompleted
        FROM reading_history rh
        LEFT JOIN chapter_archive ca
            ON rh.comic_directory_id = ca.comic_directory_fk
            AND rh.chapter_sort = ca.chapter_sort
        ORDER BY rh.updated_at DESC;
    """,
    )
    fun observeAllRecentHistoriesWithChapter(): Flow<List<ReadingHistoryWithChapter>>

    @Query("DELETE FROM reading_history WHERE comic_directory_id = :comicId")
    suspend fun deleteHistoryByDirectoryId(comicId: Long)

    @Query("UPDATE reading_history SET chapter_archive_id = :newId WHERE comic_directory_id = :comicId AND chapter_sort = :chapterSort")
    suspend fun updateHistoryChapterIdBySort(
        comicId: Long,
        chapterSort: String,
        newId: Long,
    )

    @Query("UPDATE chapter_read SET chapter_archive_id = :newId WHERE comic_directory_id = :comicId AND chapter_sort = :chapterSort")
    suspend fun updateChapterReadIdBySort(
        comicId: Long,
        chapterSort: String,
        newId: Long,
    )

    // Chapter Read
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapterRead(chapterRead: ChapterRead)

    @Query("SELECT chapter_sort FROM chapter_read WHERE comic_directory_id = :comicId")
    fun observeReadChaptersByDirectoryId(comicId: Long): Flow<List<String>>

    @Query("DELETE FROM chapter_read WHERE comic_directory_id = :comicId AND chapter_sort = :chapterSort")
    suspend fun deleteChapterRead(
        comicId: Long,
        chapterSort: String,
    )
}
