package br.acerola.manga.local.database.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterRemoteInfoDao : BaseDao<ChapterRemoteInfo> {
    @Query(value = "SELECT * FROM chapter_remote_info ORDER BY chapter ASC")
    fun getAllChaptersRemoteInfo(): Flow<List<ChapterRemoteInfo>>

    @Query(value = "SELECT * FROM chapter_remote_info WHERE id = :mangaId")
    fun getChapterRemoteInfoById(mangaId: Long): Flow<ChapterRemoteInfo?>

    @Query(value = "SELECT COUNT(id) FROM chapter_remote_info WHERE manga_remote_info_fk = :mangaId")
    suspend fun countChaptersByMangaRemoteInfo(mangaId: Long): Int

    @Query(
        value = """
            SELECT
                *
            FROM
                chapter_remote_info
            WHERE
                manga_remote_info_fk = :mangaId
            ORDER BY
              chapter ASC
            LIMIT :pageSize OFFSET :offset
        """
    )
    fun getChaptersPaged(mangaId: Long, pageSize: Int, offset: Int): Flow<List<ChapterRemoteInfo>>
}