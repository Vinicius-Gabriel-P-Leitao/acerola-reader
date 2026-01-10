package br.acerola.manga.local.database.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDirectoryDao : BaseDao<MangaDirectory>{
    @Query(value = "SELECT * FROM manga_directory ORDER BY id ASC")
    fun getAllMangaDirectory(): Flow<List<MangaDirectory>>

    @Query(value = "SELECT * FROM manga_directory WHERE id = :mangaId")
    suspend fun getMangaDirectoryById(mangaId: Long): MangaDirectory?

    @Query(value = "SELECT * FROM manga_directory WHERE name = :mangaName")
    suspend fun getMangaDirectoryByName(mangaName: String): MangaDirectory?
}