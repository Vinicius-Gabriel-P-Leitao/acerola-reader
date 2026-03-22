package br.acerola.manga.local.dao.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.entity.archive.ChapterTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterTemplateDao {
    @Query("SELECT * FROM chapter_template ORDER BY priority DESC, is_default DESC")
    fun observeAll(): Flow<List<ChapterTemplateEntity>>

    @Query("SELECT * FROM chapter_template ORDER BY priority DESC, is_default DESC")
    suspend fun getAll(): List<ChapterTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: ChapterTemplateEntity): Long

    @Query("SELECT * FROM chapter_template WHERE id = :id")
    suspend fun getById(id: Long): ChapterTemplateEntity?

    @Query("DELETE FROM chapter_template WHERE id = :id AND is_default = 0")
    suspend fun deleteCustom(id: Long): Int
}
