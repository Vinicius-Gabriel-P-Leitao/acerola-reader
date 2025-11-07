package br.acerola.manga.domain.database.dao.archive

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.acerola.manga.domain.model.archive.MangaFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaFolder(manga: MangaFolder): Long

    @Update
    suspend fun updateMangaFolder(manga: MangaFolder)

    @Delete
    suspend fun deleteMangaFolder(manga: MangaFolder)

    @Query(value = "SELECT * FROM manga_folder ORDER BY id ASC")
    fun getAllMangasFolders(): Flow<List<MangaFolder>>

    @Query(value = "SELECT * FROM manga_folder WHERE id = :mangaId")
    fun getMangaFolderById(mangaId: Int): Flow<MangaFolder?>
}