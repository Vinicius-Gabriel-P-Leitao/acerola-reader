package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.ChapterTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterTemplateDao : BaseDao<ChapterTemplate> {
    @Query("SELECT * FROM chapter_template ORDER BY priority DESC, is_default DESC")
    fun observeAllTemplates(): Flow<List<ChapterTemplate>>

    @Query("SELECT * FROM chapter_template ORDER BY priority DESC, is_default DESC")
    suspend fun getAllTemplates(): List<ChapterTemplate>

    @Query("SELECT * FROM chapter_template WHERE id = :id")
    suspend fun getTemplateById(id: Long): ChapterTemplate?

    @Query("DELETE FROM chapter_template WHERE id = :id AND is_default = 0")
    suspend fun deleteNonDefaultTemplate(id: Long): Int
}
