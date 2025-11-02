package br.acerola.manga.domain.database.dao.archive

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.acerola.manga.domain.model.archive.ChapterFile
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterFile(manga: ChapterFile)

    @Update
    suspend fun updateChapterFile(manga: ChapterFile)

    @Delete
    suspend fun deleteChapterFile(manga: ChapterFile)

    @Query("SELECT * FROM chapter_file ORDER BY chapter ASC")
    fun getAllChapterFiles(): Flow<List<ChapterFile>>

    @Query("SELECT * FROM chapter_file WHERE id = :mangaId")
    fun getChaptersFileById(mangaId: Int): Flow<ChapterFile?>
}