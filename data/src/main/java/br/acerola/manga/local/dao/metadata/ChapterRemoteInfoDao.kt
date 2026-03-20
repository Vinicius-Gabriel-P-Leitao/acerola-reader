package br.acerola.manga.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.ChapterRemoteInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterRemoteInfoDao : BaseDao<ChapterRemoteInfo> {
    @Query(value = "SELECT * FROM chapter_remote_info ORDER BY chapter ASC")
    fun getAllChaptersRemoteInfo(): Flow<List<ChapterRemoteInfo>>

    @Query(value = "SELECT * FROM chapter_remote_info WHERE id = :chapterId")
    fun getChapterRemoteInfoById(chapterId: Long): Flow<ChapterRemoteInfo?>

    @Query(value = "SELECT COUNT(id) FROM chapter_remote_info WHERE manga_remote_info_fk = :mangaId")
    suspend fun countChaptersByMangaRemoteInfo(mangaId: Long): Int

    @Query(
        value = """
        SELECT * FROM chapter_remote_info
        WHERE manga_remote_info_fk = :mangaId 
        ORDER BY chapter ASC
    """
    )
    fun getChaptersByMangaRemoteInfo(mangaId: Long): Flow<List<ChapterRemoteInfo>>

    @Query(
        value = """
        SELECT * FROM chapter_remote_info
        WHERE manga_remote_info_fk = :mangaId
        ORDER BY chapter ASC
        LIMIT :pageSize OFFSET :offset
    """
    )
    suspend fun getChaptersPaged(mangaId: Long, pageSize: Int, offset: Int): List<ChapterRemoteInfo>

    @Query("SELECT * FROM chapter_remote_info WHERE manga_remote_info_fk = :mangaId AND chapter IN (:chapters)")
    fun getChaptersByMangaAndNumbers(mangaId: Long, chapters: List<String>): Flow<List<ChapterRemoteInfo>>
}