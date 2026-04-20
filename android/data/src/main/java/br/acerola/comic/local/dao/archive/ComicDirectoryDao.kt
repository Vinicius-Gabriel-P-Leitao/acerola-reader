package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.ComicDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
interface ComicDirectoryDao : BaseDao<ComicDirectory> {
    @Query(value = "SELECT * FROM comic_directory WHERE hidden = 0 ORDER BY id ASC")
    fun getVisibleDirectories(): Flow<List<ComicDirectory>>

    @Query(value = "SELECT * FROM comic_directory ORDER BY id ASC")
    fun getAllDirectories(): Flow<List<ComicDirectory>>

    @Query(value = "SELECT * FROM comic_directory WHERE id = :mangaId")
    suspend fun getDirectoryById(mangaId: Long): ComicDirectory?

    @Query(value = "SELECT * FROM comic_directory WHERE name = :name")
    suspend fun getDirectoryByName(name: String): ComicDirectory?

    @Query(value = "UPDATE comic_directory SET hidden = :hidden WHERE id = :mangaId")
    suspend fun setDirectoryHidden(
        mangaId: Long,
        hidden: Boolean,
    )

    @Transaction
    suspend fun upsertDirectoryTransaction(
        directory: ComicDirectory,
        normalizeName: (String) -> String,
    ): Long {
        val allFolders = getAllDirectories().firstOrNull() ?: emptyList()

        val normalizedTarget = normalizeName(directory.name)
        val existing = allFolders.find { normalizeName(it.name) == normalizedTarget }

        return if (existing != null) {
            val updated = directory.copy(id = existing.id, externalSyncEnabled = existing.externalSyncEnabled, hidden = existing.hidden)
            update(entity = updated)
            existing.id
        } else {
            insert(entity = directory)
        }
    }
}
