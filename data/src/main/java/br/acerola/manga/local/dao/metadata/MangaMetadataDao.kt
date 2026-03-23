package br.acerola.manga.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.MangaMetadata
import br.acerola.manga.local.entity.relation.MetadataRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaMetadataDao : BaseDao<MangaMetadata> {

    @Query(value = "SELECT * FROM manga_metadata ORDER BY id ASC")
    fun getAllMangaRemoteInfo(): Flow<List<MangaMetadata>>

    @Query(value = "SELECT * FROM manga_metadata WHERE title = :title")
    fun getMangaRemoteInfoByName(title: String): Flow<MangaMetadata?>

    @Query(value = "SELECT * FROM manga_metadata WHERE id = :mangaId")
    fun getMangaById(mangaId: Long): Flow<MangaMetadata?>

    @Query(value = "SELECT * FROM manga_metadata WHERE manga_directory_fk = :directoryId")
    fun getMangaByDirectoryId(directoryId: Long): Flow<MangaMetadata?>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata WHERE id = :mangaId")
    fun getMangaWithRelationsById(mangaId: Long): Flow<MetadataRelations?>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata WHERE manga_directory_fk = :directoryId")
    fun getMangaWithRelationsByDirectoryId(directoryId: Long): Flow<MetadataRelations?>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata ORDER BY title ASC")
    fun getAllMangasWithRelations(): Flow<List<MetadataRelations>>
}
