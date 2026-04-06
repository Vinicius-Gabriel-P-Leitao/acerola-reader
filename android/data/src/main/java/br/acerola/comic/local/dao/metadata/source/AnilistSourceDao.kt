package br.acerola.comic.local.dao.metadata.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.source.AnilistSource

@Dao
interface AnilistSourceDao : BaseDao<AnilistSource> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: AnilistSource): Long

    @Query(value = "SELECT * FROM anilist_source WHERE comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMangaRemoteInfoFk(mangaRemoteInfoFk: Long): AnilistSource?
}
