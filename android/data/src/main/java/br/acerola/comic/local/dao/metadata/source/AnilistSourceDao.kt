package br.acerola.comic.local.dao.metadata.source

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.source.AnilistSource

@Dao
interface AnilistSourceDao : BaseDao<AnilistSource> {
    @Query(value = "SELECT * FROM anilist_source WHERE comic_metadata_fk = :comicRemoteInfoFk LIMIT 1")
    suspend fun getByMetadataId(comicRemoteInfoFk: Long): AnilistSource?
}
