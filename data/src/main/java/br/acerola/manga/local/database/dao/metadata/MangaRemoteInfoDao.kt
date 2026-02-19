package br.acerola.manga.local.database.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.relation.RemoteInfoRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaRemoteInfoDao : BaseDao<MangaRemoteInfo> {

    @Query(value = "SELECT * FROM manga_remote_info ORDER BY id ASC")
    fun getAllMangaRemoteInfo(): Flow<List<MangaRemoteInfo>>

    @Query(value = "SELECT * FROM manga_remote_info WHERE title = :title")
    fun getMangaRemoteInfoByName(title: String): Flow<MangaRemoteInfo?>

    @Query(value = "SELECT * FROM manga_remote_info WHERE id = :mangaId")
    fun getMangaById(mangaId: Long): Flow<MangaRemoteInfo?>

    @Transaction
    @Query(value = "SELECT * FROM manga_remote_info WHERE id = :mangaId")
    fun getMangaWithRelationsById(mangaId: Long): Flow<br.acerola.manga.local.database.entity.relation.RemoteInfoRelations?>

    @Transaction
    @Query(value = "SELECT * FROM manga_remote_info ORDER BY title ASC")
    fun getAllMangasWithRelations(): Flow<List<RemoteInfoRelations>>
}