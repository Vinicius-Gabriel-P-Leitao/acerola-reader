package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterArchiveDao : BaseDao<ChapterArchive> {

    @Query(value = "DELETE FROM chapter_archive WHERE comic_directory_fk = :folderId")
    suspend fun deleteChaptersByComicDirectoryId(folderId: Long)

    @Query(value = "SELECT * FROM chapter_archive ORDER BY chapter ASC")
    fun getAllChapterFiles(): Flow<List<ChapterArchive>>

    @Query(value = "SELECT * FROM chapter_archive WHERE id = :chapterId")
    fun getChaptersFileById(chapterId: Long): Flow<ChapterArchive?>

    @Query(value = "SELECT COUNT(id) FROM chapter_archive WHERE comic_directory_fk = :folderId")
    suspend fun countChaptersByComicDirectory(folderId: Long): Int

    @Query("SELECT * FROM chapter_archive WHERE comic_directory_fk = :folderId")
    suspend fun getChaptersByComicDirectoryList(folderId: Long): List<ChapterArchive>

    @Query(
        value = """
        SELECT *
        FROM chapter_archive
        WHERE comic_directory_fk = :folderId 
        ORDER BY 
            -- NOTE: Ordena pela parte inteira (antes do ponto)
            CAST(chapter_sort AS INTEGER) ASC, 
            -- NOTE: Ordena pela parte decimal (depois do ponto) como um número inteiro
            CAST(
                CASE 
                    WHEN chapter_sort LIKE '%.%' 
                    THEN SUBSTR(chapter_sort, INSTR(chapter_sort, '.') + 1) 
                    ELSE 0 
                END AS INTEGER
            ) ASC
    """
    )
    fun getChaptersByComicDirectory(folderId: Long): Flow<List<ChapterArchive>>

    @Query(
        value = """
            SELECT *
            FROM chapter_archive
            WHERE comic_directory_fk = :folderId
            ORDER BY 
                -- NOTE: Ordena pela parte inteira (antes do ponto)
                CAST(chapter_sort AS INTEGER) ASC, 
                -- NOTE: Ordena pela parte decimal (depois do ponto) como um número inteiro
                CAST(
                    CASE    
                        WHEN chapter_sort LIKE '%.%' 
                        THEN SUBSTR(chapter_sort, INSTR(chapter_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) ASC
            LIMIT :pageSize OFFSET :offset
        """
    )
    suspend fun getChaptersPaged(
        folderId: Long,
        pageSize: Int,
        offset: Int
    ): List<ChapterArchive>

    @Query("SELECT * FROM chapter_archive WHERE comic_directory_fk = :folderId AND chapter_sort IN (:chapters)")
    fun getChaptersByComicAndSorts(
        folderId: Long,
        chapters: List<String>
    ): Flow<List<ChapterArchive>>

    @Query("SELECT comic_directory_fk, COUNT(*) as count FROM chapter_archive GROUP BY comic_directory_fk")
    fun getAllChapterCounts(): Flow<List<MangaChapterCount>>
}

// FIXME: Colocado de forma errada, além de estar com snakecase tá aqui deveria estar em outra pasta.
data class MangaChapterCount(
    val comic_directory_fk: Long,
    val count: Int
)