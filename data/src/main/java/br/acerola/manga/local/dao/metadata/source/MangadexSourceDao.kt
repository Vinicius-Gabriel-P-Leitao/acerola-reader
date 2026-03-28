package br.acerola.manga.local.dao.metadata.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.source.MangadexSource

@Dao
interface MangadexSourceDao : BaseDao<MangadexSource> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: MangadexSource): Long

    @Query(value = "SELECT * FROM mangadex_source WHERE manga_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMangaRemoteInfoFk(mangaRemoteInfoFk: Long): MangadexSource?
}
