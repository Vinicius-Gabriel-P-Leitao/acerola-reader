package br.acerola.manga.domain.database.dao.database.archive

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.acerola.manga.domain.database.dao.database.BaseDao
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterFileDao : BaseDao<ChapterFile> {
    @Query(
        value = """
            DELETE FROM
                chapter_file
            WHERE
                folder_path_fk = :folderId
        """
    )
    suspend fun deleteChaptersByFolderId(folderId: Long)

    @Query(
        value = """
            SELECT
                *
            FROM
                chapter_file
            ORDER BY
                chapter ASC
        """
    )
    fun getAllChapterFiles(): Flow<List<ChapterFile>>

    @Query(
        value = """
            SELECT
                *
            FROM
                chapter_file
            WHERE
                id = :chapterId
        """
    )
    fun getChaptersFileById(chapterId: Long): Flow<ChapterFile?>

    @Query(
        value = """
            SELECT
                COUNT(id)
            FROM
                chapter_file
            WHERE
                folder_path_fk = :folderId
        """
    )
    suspend fun countChaptersByFolder(folderId: Long): Int

    @Query(
        value = """
            SELECT
                *
            FROM
                chapter_file
            WHERE
                folder_path_fk = :folderId
            ORDER BY
               CAST(REPLACE(chapter_sort, ',', '.') AS REAL) ASC
        """
    )
    fun getChaptersByFolder(folderId: Long): Flow<List<ChapterFile>>

    @Query(
        value = """
            SELECT
                *
            FROM
                chapter_file
            WHERE
                folder_path_fk = :folderId
            ORDER BY
               CAST(REPLACE(chapter_sort, ',', '.') AS REAL) ASC
            LIMIT :pageSize OFFSET :offset
        """
    )
    fun getChaptersPaged(folderId: Long, pageSize: Int, offset: Int): Flow<List<ChapterFile>>
}