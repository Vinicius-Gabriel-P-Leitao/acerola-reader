package br.acerola.manga.domain.database.dao.metadata

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.acerola.manga.domain.model.metadata.ChapterMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterMetadata(manga: ChapterMetadata): Long

    @Update
    suspend fun updateChapterMetadata(manga: ChapterMetadata)

    @Delete
    suspend fun deleteChapterMetadata(manga: ChapterMetadata)

    @Query("SELECT * FROM chapter_metadata ORDER BY chapter ASC")
    fun getAllChaptersMetadata(): Flow<List<ChapterMetadata>>

    @Query("SELECT * FROM chapter_metadata WHERE id = :mangaId")
    fun getChapterMetadataById(mangaId: Int): Flow<ChapterMetadata?>
}