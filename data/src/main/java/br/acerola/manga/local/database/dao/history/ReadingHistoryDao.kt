package br.acerola.manga.local.database.dao.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.database.entity.history.ReadingHistory
import br.acerola.manga.local.database.entity.history.ReadingHistoryWithChapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: ReadingHistory)

    @Query("SELECT * FROM reading_history WHERE manga_id = :mangaId")
    fun getByMangaId(mangaId: Long): Flow<ReadingHistory?>

    @Query("SELECT * FROM reading_history ORDER BY updated_at DESC")
    fun getAllRecent(): Flow<List<ReadingHistory>>

    @Query("""
        SELECT rh.manga_id as mangaId, rh.chapter_id as chapterId, rh.last_page as lastPage, rh.updated_at as updatedAt, ca.chapter as chapterName
        FROM reading_history rh
        LEFT JOIN chapter_archive ca ON rh.chapter_id = ca.id
        ORDER BY rh.updated_at DESC
    """)
    fun getAllRecentWithChapterName(): Flow<List<ReadingHistoryWithChapter>>

    @Query("DELETE FROM reading_history WHERE manga_id = :mangaId")
    suspend fun deleteByMangaId(mangaId: Long)
}
