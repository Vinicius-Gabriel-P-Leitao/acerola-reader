package br.acerola.manga.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.archive.MangaDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
interface MangaDirectoryDao : BaseDao<MangaDirectory>{
    @Query(value = "SELECT * FROM manga_directory WHERE hidden = 0 ORDER BY id ASC")
    fun getAllMangaDirectory(): Flow<List<MangaDirectory>>

    @Query(value = "SELECT * FROM manga_directory ORDER BY id ASC")
    fun getAllMangaDirectoryIncludingHidden(): Flow<List<MangaDirectory>>

    @Query(value = "SELECT * FROM manga_directory WHERE id = :mangaId")
    suspend fun getMangaDirectoryById(mangaId: Long): MangaDirectory?

    @Query(value = "SELECT * FROM manga_directory WHERE name = :name")
    suspend fun getMangaDirectoryByName(name: String): MangaDirectory?

    @Query(value = "UPDATE manga_directory SET hidden = :hidden WHERE id = :mangaId")
    suspend fun setHidden(mangaId: Long, hidden: Boolean)

    @Transaction
    suspend fun upsertMangaDirectoryTransaction(
        directory: MangaDirectory,
        normalizeName: (String) -> String
    ): Long {
        val allFolders = getAllMangaDirectoryIncludingHidden().firstOrNull() ?: emptyList()

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
