package br.acerola.comic.local.dao.metadata.source

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.source.ComicInfoSource

@Dao
interface ComicInfoSourceDao : BaseDao<ComicInfoSource> {
    @Query(value = "SELECT * FROM comic_info_source WHERE comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMetadataId(mangaRemoteInfoFk: Long): ComicInfoSource?
}
