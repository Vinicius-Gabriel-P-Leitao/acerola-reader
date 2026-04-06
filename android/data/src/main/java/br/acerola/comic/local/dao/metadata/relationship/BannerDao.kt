package br.acerola.comic.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.relationship.Banner

@Dao
interface BannerDao : BaseDao<Banner> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Banner): Long

    @Query(value = "SELECT * FROM banner WHERE file_name = :fileName AND comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getBannerByFileNameAndFk(fileName: String, mangaRemoteInfoFk: Long): Banner?
}
