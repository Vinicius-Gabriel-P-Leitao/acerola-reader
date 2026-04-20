package br.acerola.comic.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.relationship.Cover

@Dao
interface CoverDao : BaseDao<Cover> {
    @Query(value = "SELECT * FROM cover WHERE file_name = :fileName AND comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getByFileNameAndMetadataId(
        fileName: String,
        mangaRemoteInfoFk: Long,
    ): Cover?
}
