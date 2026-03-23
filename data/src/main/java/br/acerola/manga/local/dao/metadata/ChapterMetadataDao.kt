package br.acerola.manga.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.ChapterMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterMetadataDao : BaseDao<ChapterMetadata> {
    @Query(value = "SELECT * FROM chapter_metadata ORDER BY chapter ASC")
    fun getAllChaptersRemoteInfo(): Flow<List<ChapterMetadata>>

    @Query(value = "SELECT * FROM chapter_metadata WHERE id = :chapterId")
    fun getChapterRemoteInfoById(chapterId: Long): Flow<ChapterMetadata?>

    @Query(value = "SELECT COUNT(id) FROM chapter_metadata WHERE manga_metadata_fk = :mangaId")
    suspend fun countChaptersByMangaRemoteInfo(mangaId: Long): Int

    @Query(
        value = """
        SELECT * FROM chapter_metadata
        WHERE manga_metadata_fk = :mangaId 
        ORDER BY chapter ASC
    """
    )
    fun getChaptersByMangaRemoteInfo(mangaId: Long): Flow<List<ChapterMetadata>>

    @Query(
        value = """
        SELECT * FROM chapter_metadata
        WHERE manga_metadata_fk = :mangaId
        ORDER BY chapter ASC
        LIMIT :pageSize OFFSET :offset
    """
    )
    suspend fun getChaptersPaged(mangaId: Long, pageSize: Int, offset: Int): List<ChapterMetadata>

    @Query("SELECT * FROM chapter_metadata WHERE manga_metadata_fk = :mangaId AND chapter IN (:chapters)")
    fun getChaptersByMangaAndNumbers(mangaId: Long, chapters: List<String>): Flow<List<ChapterMetadata>>
}