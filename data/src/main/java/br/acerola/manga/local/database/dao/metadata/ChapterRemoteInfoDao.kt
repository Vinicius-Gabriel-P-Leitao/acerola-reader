package br.acerola.manga.local.database.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterRemoteInfoDao : BaseDao<ChapterRemoteInfo> {
    @Query("SELECT * FROM chapter_remote_info ORDER BY chapter ASC")
    fun getAllChaptersRemoteInfo(): Flow<List<ChapterRemoteInfo>>

    @Query("SELECT * FROM chapter_remote_info WHERE id = :mangaId")
    fun getChapterRemoteInfoById(mangaId: Long): Flow<ChapterRemoteInfo?>
}