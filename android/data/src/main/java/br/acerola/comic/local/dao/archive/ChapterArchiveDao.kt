package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.relation.ChapterVolumeJoin
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

    @Query("SELECT COUNT(*) FROM chapter_archive WHERE comic_directory_fk = :folderId AND volume_id_fk IS NULL")
    suspend fun countRootChaptersByDirectoryId(folderId: Long): Int

    @Transaction
    @Query(
        value = """
        SELECT *
        FROM chapter_archive
        LEFT JOIN volume_archive ON chapter_archive.volume_id_fk = volume_archive.id
        WHERE chapter_archive.comic_directory_fk = :folderId 
        ORDER BY 
            -- 1. Especiais (Volumes ou Chapters) por último
            (chapter_archive.is_special OR COALESCE(volume_archive.is_special, 0)) ASC,
            -- 2. Volume Parte Inteira
            CAST(COALESCE(volume_archive.volume_sort, '0') AS INTEGER) ASC,
            -- 3. Volume Parte Decimal
            CAST(
                CASE 
                    WHEN volume_archive.volume_sort LIKE '%.%' 
                    THEN SUBSTR(volume_archive.volume_sort, INSTR(volume_archive.volume_sort, '.') + 1) 
                    ELSE 0 
                END AS INTEGER
            ) ASC,
            -- 4. Chapter Parte Inteira
            CAST(chapter_archive.chapter_sort AS INTEGER) ASC, 
            -- 5. Chapter Parte Decimal
            CAST(
                CASE 
                    WHEN chapter_archive.chapter_sort LIKE '%.%' 
                    THEN SUBSTR(chapter_archive.chapter_sort, INSTR(chapter_archive.chapter_sort, '.') + 1) 
                    ELSE 0 
                END AS INTEGER
            ) ASC
    """,
    )
    fun getChaptersByDirectoryId(folderId: Long): Flow<List<ChapterVolumeJoin>>

    @Transaction
    @Query(
        value = """
            SELECT *
            FROM chapter_archive
            LEFT JOIN volume_archive ON chapter_archive.volume_id_fk = volume_archive.id
            WHERE chapter_archive.comic_directory_fk = :folderId
            ORDER BY 
                -- 1. Especiais (Volumes ou Chapters) por último
                (chapter_archive.is_special OR COALESCE(volume_archive.is_special, 0)) ASC,
                -- 2. Volume Parte Inteira
                CAST(COALESCE(volume_archive.volume_sort, '0') AS INTEGER) ASC,
                -- 3. Volume Parte Decimal
                CAST(
                    CASE 
                        WHEN volume_archive.volume_sort LIKE '%.%' 
                        THEN SUBSTR(volume_archive.volume_sort, INSTR(volume_archive.volume_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) ASC,
                -- 4. Chapter Parte Inteira
                CAST(chapter_archive.chapter_sort AS INTEGER) ASC, 
                -- 5. Chapter Parte Decimal
                CAST(
                    CASE 
                        WHEN chapter_archive.chapter_sort LIKE '%.%' 
                        THEN SUBSTR(chapter_archive.chapter_sort, INSTR(chapter_archive.chapter_sort, '.') + 1) 
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
    ): List<ChapterVolumeJoin>

    @Transaction
    @Query(
        value = """
            SELECT *
            FROM chapter_archive
            LEFT JOIN volume_archive ON chapter_archive.volume_id_fk = volume_archive.id
            WHERE chapter_archive.comic_directory_fk = :comicId
              AND chapter_archive.volume_id_fk = :volumeId
            ORDER BY 
                (chapter_archive.is_special OR COALESCE(volume_archive.is_special, 0)) ASC,
                CAST(chapter_archive.chapter_sort AS INTEGER) ASC,
                CAST(
                    CASE 
                        WHEN chapter_archive.chapter_sort LIKE '%.%' 
                        THEN SUBSTR(chapter_archive.chapter_sort, INSTR(chapter_archive.chapter_sort, '.') + 1) 
                        ELSE 0 
                    END AS INTEGER
                ) ASC
            LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun getChaptersByVolumePaged(
        comicId: Long,
        volumeId: Long,
        pageSize: Int,
        offset: Int,
    ): List<ChapterVolumeJoin>

    @Query("SELECT * FROM chapter_archive WHERE comic_directory_fk = :folderId AND chapter_sort IN (:chapters)")
    fun getChaptersByDirectoryAndSorts(
        folderId: Long,
        chapters: List<String>,
    ): Flow<List<ChapterArchive>>

    @Query("SELECT comic_directory_fk, COUNT(*) as count FROM chapter_archive GROUP BY comic_directory_fk")
    fun getChapterCountsByDirectory(): Flow<List<MangaChapterCount>>
}
