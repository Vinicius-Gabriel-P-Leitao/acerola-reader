package br.acerola.manga.domain.database.dao.database.metadata

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
    suspend fun insertMangaMetadata(manga: MangaMetadata): Long

    @Update
    suspend fun updateMangaMetadata(manga: MangaMetadata)

    @Delete
    suspend fun deleteMangaMetadata(manga: MangaMetadata)

    @Query(value = "SELECT * FROM manga_metadata ORDER BY id ASC")
    fun getAllMangasMetadata(): Flow<List<MangaMetadata>>

    @Query(value = "SELECT * FROM manga_metadata WHERE name = :name")
    fun getMangaMetadataByName(name: String): Flow<MangaMetadata?>
}