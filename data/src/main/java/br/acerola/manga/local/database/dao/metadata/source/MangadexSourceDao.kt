package br.acerola.manga.local.database.dao.metadata.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.source.MangadexSource

@Dao
interface MangadexSourceDao : BaseDao<MangadexSource> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: MangadexSource): Long

    @Query(value = "SELECT * FROM mangadex_source WHERE manga_remote_info_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMangaRemoteInfoFk(mangaRemoteInfoFk: Long): MangadexSource?
}
