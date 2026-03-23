package br.acerola.manga.local.dao.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.entity.archive.ChapterTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterTemplateDao {
    @Query("SELECT * FROM chapter_template ORDER BY priority DESC, is_default DESC")
    fun observeAll(): Flow<List<ChapterTemplate>>

    @Query("SELECT * FROM chapter_template ORDER BY priority DESC, is_default DESC")
    suspend fun getAll(): List<ChapterTemplate>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: ChapterTemplate): Long

    @Query("SELECT * FROM chapter_template WHERE id = :id")
    suspend fun getById(id: Long): ChapterTemplate?

    @Query("DELETE FROM chapter_template WHERE id = :id AND is_default = 0")
    suspend fun deleteCustom(id: Long): Int
}
