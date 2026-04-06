package br.acerola.comic.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.ChapterDownloadSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDownloadSourceDao : BaseDao<ChapterDownloadSource> {
    @Query(value = "SELECT * FROM chapter_page ORDER BY id ASC")
    fun getAllChapterDownloadSource(): Flow<List<ChapterDownloadSource>>

    @Query(value = "SELECT * FROM chapter_page WHERE id = :chapterId")
    fun getChapterDownloadSourceById(chapterId: Long): Flow<ChapterDownloadSource?>

    @Query(value = "SELECT * FROM chapter_page WHERE chapter_fk IN (:chapterId)")
    fun getChapterDownloadSourceByRemoteInfoId(chapterId: List<Long>): Flow<List<ChapterDownloadSource>>
}