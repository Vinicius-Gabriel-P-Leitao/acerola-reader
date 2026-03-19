package br.acerola.manga.local.dao.metadata.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource

@Dao
interface ComicInfoSourceDao : BaseDao<ComicInfoSource> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: ComicInfoSource): Long

    @Query(value = "SELECT * FROM comic_info_source WHERE manga_remote_info_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByMangaRemoteInfoFk(mangaRemoteInfoFk: Long): ComicInfoSource?
}
