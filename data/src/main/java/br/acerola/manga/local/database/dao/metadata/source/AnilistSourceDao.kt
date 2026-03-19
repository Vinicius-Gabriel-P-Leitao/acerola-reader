package br.acerola.manga.local.database.dao.metadata.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.source.AnilistSource

@Dao
interface AnilistSourceDao : BaseDao<AnilistSource> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: AnilistSource): Long

    @Query(value = "SELECT * FROM anilist_source WHERE manga_remote_info_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMangaRemoteInfoFk(mangaRemoteInfoFk: Long): AnilistSource?
}
