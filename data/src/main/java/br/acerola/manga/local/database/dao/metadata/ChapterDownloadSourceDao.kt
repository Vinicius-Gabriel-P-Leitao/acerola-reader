package br.acerola.manga.local.database.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDownloadSourceDao: BaseDao<ChapterDownloadSource> {
    @Query("SELECT * FROM chapter_page ORDER BY id ASC")
    fun getAllChapterDownloadSource(): Flow<List<ChapterDownloadSource>>

    @Query("SELECT * FROM chapter_page WHERE id = :mangaId")
    fun getChapterDownloadSourceById(mangaId: Long): Flow<ChapterDownloadSource?>
}