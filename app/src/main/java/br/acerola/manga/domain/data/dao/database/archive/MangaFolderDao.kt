package br.acerola.manga.domain.data.dao.database.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.domain.data.dao.database.BaseDao
import br.acerola.manga.domain.model.archive.MangaFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaFolderDao : BaseDao<MangaFolder>{
    @Query(value = "SELECT * FROM manga_folder ORDER BY id ASC")
    fun getAllMangasFolders(): Flow<List<MangaFolder>>

    @Query(value = "SELECT * FROM manga_folder WHERE id = :mangaId")
    suspend fun getMangaFolderById(mangaId: Long): MangaFolder?
}