package br.acerola.comic.local.dao.view

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.entity.view.ComicSummaryView
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicSummaryDao {
    @Query("SELECT * FROM comic_summary_view ORDER BY metadata_title ASC, folder_name ASC")
    fun getAllMangaSummaries(): Flow<List<ComicSummaryView>>
}
