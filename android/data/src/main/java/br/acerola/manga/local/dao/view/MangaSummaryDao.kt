package br.acerola.manga.local.dao.view

import androidx.room.Dao
import androidx.room.Query
import br.acerola.manga.local.entity.view.MangaSummaryView
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaSummaryDao {
    @Query("SELECT * FROM manga_summary_view ORDER BY metadata_title ASC, folder_name ASC")
    fun getAllMangaSummaries(): Flow<List<MangaSummaryView>>
}
