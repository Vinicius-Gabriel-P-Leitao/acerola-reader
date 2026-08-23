package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.ArchiveTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveTemplateDao : BaseDao<ArchiveTemplate> {
    @Query("SELECT * FROM archive_template ORDER BY priority DESC, is_default DESC")
    fun observeAllTemplates(): Flow<List<ArchiveTemplate>>

    @Query("SELECT * FROM archive_template ORDER BY priority DESC, is_default DESC")
    suspend fun getAllTemplates(): List<ArchiveTemplate>

    @Query("SELECT * FROM archive_template WHERE id = :id")
    suspend fun getTemplateById(id: Long): ArchiveTemplate?

    @Query("DELETE FROM archive_template WHERE id = :id AND is_default = 0")
    suspend fun deleteNonDefaultTemplate(id: Long): Int
}
