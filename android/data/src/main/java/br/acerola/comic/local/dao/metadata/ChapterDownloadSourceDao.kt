package br.acerola.comic.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.ChapterDownloadSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDownloadSourceDao : BaseDao<ChapterDownloadSource> {
    @Query(value = "SELECT * FROM chapter_page ORDER BY id ASC")
    fun observeAllChapterDownloadSources(): Flow<List<ChapterDownloadSource>>

    @Query(value = "SELECT * FROM chapter_page WHERE id = :chapterId")
    fun observeChapterDownloadSourceById(chapterId: Long): Flow<ChapterDownloadSource?>

    @Query(value = "SELECT * FROM chapter_page WHERE chapter_fk IN (:chapterId)")
    fun observeChapterDownloadSourcesByChapterIds(chapterId: List<Long>): Flow<List<ChapterDownloadSource>>
}
