package br.acerola.manga.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.relationship.Banner

@Dao
interface BannerDao : BaseDao<Banner> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Banner): Long

    @Query(value = "SELECT * FROM banner WHERE file_name = :fileName AND manga_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getBannerByFileNameAndFk(fileName: String, mangaRemoteInfoFk: Long): Banner?
}
