package br.acerola.manga.domain.database.dao.database.archive

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.acerola.manga.domain.database.dao.database.BaseDao
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.model.metadata.MangaMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaFolderDao : BaseDao<MangaFolder>{
    @Query(value = "SELECT * FROM manga_folder ORDER BY id ASC")
    fun getAllMangasFolders(): Flow<List<MangaFolder>>

    @Query(value = "SELECT * FROM manga_folder WHERE id = :mangaId")
    suspend fun getMangaFolderById(mangaId: Long): MangaFolder?
}