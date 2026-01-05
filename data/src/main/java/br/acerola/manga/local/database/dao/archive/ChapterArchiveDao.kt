package br.acerola.manga.local.database.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterArchiveDao : BaseDao<ChapterArchive> {
    @Query(value = "DELETE FROM chapter_archive WHERE folder_path_fk = :folderId")
    suspend fun deleteChaptersByMangaDirectoryId(folderId: Long)

    @Query(value = "SELECT * FROM chapter_archive ORDER BY chapter ASC")
    fun getAllChapterFiles(): Flow<List<ChapterArchive>>

    @Query(value = "SELECT * FROM chapter_archive WHERE id = :chapterId")
    fun getChaptersFileById(chapterId: Long): Flow<ChapterArchive?>

    @Query(value = "SELECT COUNT(id) FROM chapter_archive WHERE folder_path_fk = :folderId")
    suspend fun countChaptersByMangaDirectory(folderId: Long): Int

    @Query(
        value = """
            SELECT *
            FROM chapter_archive
            WHERE folder_path_fk = :folderId 
            ORDER BY CAST(REPLACE(chapter_sort, ',', '.') AS REAL) ASC
        """
    )
    fun getChaptersByMangaDirectory(folderId: Long): Flow<List<ChapterArchive>>

    @Query(
        value = """
            SELECT *
            FROM chapter_archive
            WHERE folder_path_fk = :folderId
            ORDER BY CAST(REPLACE(chapter_sort, ',', '.') AS REAL) ASC
            LIMIT :pageSize OFFSET :offset
        """
    )
    suspend fun getChaptersPaged(folderId: Long, pageSize: Int, offset: Int): List<ChapterArchive>
}