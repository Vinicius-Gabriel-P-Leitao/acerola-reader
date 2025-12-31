package br.acerola.manga.data.local.database.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.data.local.database.dao.BaseDao
import br.acerola.manga.domain.model.archive.MangaFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaFolderDao : BaseDao<MangaFolder>{
    @Query(value = "SELECT * FROM manga_folder ORDER BY id ASC")
    fun getAllMangasFolders(): Flow<List<MangaFolder>>

    @Query(value = "SELECT * FROM manga_folder WHERE id = :mangaId")
    suspend fun getMangaFolderById(mangaId: Long): MangaFolder?
}