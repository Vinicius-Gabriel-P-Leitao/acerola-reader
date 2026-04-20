package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.relation.MangaChapterCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterArchiveDao : BaseDao<ChapterArchive> {
    @Query(value = "DELETE FROM chapter_archive WHERE comic_directory_fk = :folderId")
    suspend fun deleteByDirectoryId(folderId: Long)

    @Query(value = "SELECT * FROM chapter_archive ORDER BY chapter ASC")
    fun getAllChapters(): Flow<List<ChapterArchive>>

    @Query(value = "SELECT * FROM chapter_archive WHERE id = :chapterId")
    fun getChapterById(chapterId: Long): Flow<ChapterArchive?>

    @Query(value = "SELECT COUNT(id) FROM chapter_archive WHERE comic_directory_fk = :folderId")
    suspend fun countByDirectoryId(folderId: Long): Int

    @Query("SELECT * FROM chapter_archive WHERE comic_directory_fk = :folderId")
    suspend fun getChaptersListByDirectoryId(folderId: Long): List<ChapterArchive>

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
    """,
    )
    fun getChaptersByDirectoryId(folderId: Long): Flow<List<ChapterArchive>>

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
        """,
    )
    suspend fun getChaptersByDirectoryPaged(
        folderId: Long,
        pageSize: Int,
        offset: Int,
    ): List<ChapterArchive>

    @Query("SELECT * FROM chapter_archive WHERE comic_directory_fk = :folderId AND chapter_sort IN (:chapters)")
    fun getChaptersByDirectoryAndSorts(
        folderId: Long,
        chapters: List<String>,
    ): Flow<List<ChapterArchive>>

    @Query("SELECT comic_directory_fk, COUNT(*) as count FROM chapter_archive GROUP BY comic_directory_fk")
    fun getChapterCountsByDirectory(): Flow<List<MangaChapterCount>>
}
