package br.acerola.manga.domain.database.dao.metadata

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.acerola.manga.domain.model.metadata.MangaMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaMetadata(manga: MangaMetadata)

    @Update
    suspend fun updateMangaMetadata(manga: MangaMetadata)

    @Delete
    suspend fun deleteMangaMetadata(manga: MangaMetadata)

    @Query("SELECT * FROM manga_metadata ORDER BY id ASC")
    fun getAllMangasMetadata(): Flow<List<MangaMetadata>>

    @Query("SELECT * FROM manga_metadata WHERE id = :mangaId")
    fun getMangaMetadataById(mangaId: Int): Flow<MangaMetadata?>
}