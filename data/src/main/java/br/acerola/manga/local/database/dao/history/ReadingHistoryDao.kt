package br.acerola.manga.local.database.dao.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.database.entity.history.ReadingHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: ReadingHistory)

    @Query("SELECT * FROM reading_history WHERE manga_id = :mangaId")
    fun getByMangaId(mangaId: Long): Flow<ReadingHistory?>

    @Query("SELECT * FROM reading_history ORDER BY updated_at DESC")
    fun getAllRecent(): Flow<List<ReadingHistory>>

    @Query("DELETE FROM reading_history WHERE manga_id = :mangaId")
    suspend fun deleteByMangaId(mangaId: Long)
}
