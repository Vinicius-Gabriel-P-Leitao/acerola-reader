package br.acerola.comic.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.ChapterMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterMetadataDao : BaseDao<ChapterMetadata> {
    @Query(value = "SELECT * FROM chapter_metadata ORDER BY chapter ASC")
    fun observeAllChapters(): Flow<List<ChapterMetadata>>

    @Query(value = "SELECT * FROM chapter_metadata WHERE id = :chapterId")
    fun observeChapterById(chapterId: Long): Flow<ChapterMetadata?>

    @Query(value = "SELECT COUNT(id) FROM chapter_metadata WHERE comic_metadata_fk = :comicId")
    suspend fun countChaptersByMetadataId(comicId: Long): Int

    @Query("SELECT * FROM chapter_metadata WHERE comic_metadata_fk = :comicId ORDER BY chapter ASC")
    suspend fun getChaptersListByMetadataId(comicId: Long): List<ChapterMetadata>

    @Query(
        value = """
        SELECT * FROM chapter_metadata
        WHERE comic_metadata_fk = :comicId 
        ORDER BY chapter ASC
        """,
    )
    fun observeChaptersByMetadataId(comicId: Long): Flow<List<ChapterMetadata>>

    @Query(
        value = """
        SELECT * FROM chapter_metadata
        WHERE comic_metadata_fk = :comicId
        ORDER BY chapter ASC
        LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun getChaptersByMetadataPaged(
        comicId: Long,
        pageSize: Int,
        offset: Int,
    ): List<ChapterMetadata>

    @Query("SELECT * FROM chapter_metadata WHERE comic_metadata_fk = :comicId AND chapter IN (:chapters)")
    fun observeChaptersByMetadataAndNumbers(
        comicId: Long,
        chapters: List<String>,
    ): Flow<List<ChapterMetadata>>
}
