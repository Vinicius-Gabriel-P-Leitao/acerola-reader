package br.acerola.manga.domain.database.dao.database.metadata

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.acerola.manga.domain.database.dao.database.BaseDao
import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.domain.model.relation.MetadataWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaMetadataDao : BaseDao<MangaMetadata> {

    @Query(value = "SELECT * FROM manga_metadata ORDER BY id ASC")
    fun getAllMangasMetadata(): Flow<List<MangaMetadata>>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata ORDER BY name ASC")
    fun getAllMangasWithRelations(): Flow<List<MetadataWithRelations>>

    @Query(value = "SELECT * FROM manga_metadata WHERE name = :name")
    fun getMangaMetadataByName(name: String): Flow<MangaMetadata?>
}