package br.acerola.comic.local.dao.metadata.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.source.ComicInfoSource

@Dao
interface ComicInfoSourceDao : BaseDao<ComicInfoSource> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: ComicInfoSource): Long

    @Query(value = "SELECT * FROM comic_info_source WHERE comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMangaRemoteInfoFk(mangaRemoteInfoFk: Long): ComicInfoSource?
}
