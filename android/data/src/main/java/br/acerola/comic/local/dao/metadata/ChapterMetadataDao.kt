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

    @Query(value = "SELECT COUNT(id) FROM chapter_metadata WHERE comic_metadata_fk = :mangaId")
    suspend fun countChaptersByMetadataId(mangaId: Long): Int

    @Query(
        value = """
        SELECT * FROM chapter_metadata
        WHERE comic_metadata_fk = :mangaId 
        ORDER BY chapter ASC
    """,
    )
    fun observeChaptersByMetadataId(mangaId: Long): Flow<List<ChapterMetadata>>

    @Query(
        value = """
        SELECT * FROM chapter_metadata
        WHERE comic_metadata_fk = :mangaId
        ORDER BY chapter ASC
        LIMIT :pageSize OFFSET :offset
    """,
    )
    suspend fun getChaptersByMetadataPaged(
        mangaId: Long,
        pageSize: Int,
        offset: Int,
    ): List<ChapterMetadata>

    @Query("SELECT * FROM chapter_metadata WHERE comic_metadata_fk = :mangaId AND chapter IN (:chapters)")
    fun observeChaptersByMetadataAndNumbers(
        mangaId: Long,
        chapters: List<String>,
    ): Flow<List<ChapterMetadata>>
}
