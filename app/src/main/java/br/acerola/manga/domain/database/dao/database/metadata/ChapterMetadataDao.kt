package br.acerola.manga.domain.database.dao.database.metadata

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.domain.database.dao.database.BaseDao
import br.acerola.manga.domain.model.metadata.ChapterMetadata
import br.acerola.manga.domain.model.relation.MetadataWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterMetadataDao : BaseDao<ChapterMetadata> {


    @Query("SELECT * FROM chapter_metadata ORDER BY chapter ASC")
    fun getAllChaptersMetadata(): Flow<List<ChapterMetadata>>

    @Query("SELECT * FROM chapter_metadata WHERE id = :mangaId")
    fun getChapterMetadataById(mangaId: Int): Flow<ChapterMetadata?>
}