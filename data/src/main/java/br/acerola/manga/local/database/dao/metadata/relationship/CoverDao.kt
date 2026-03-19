package br.acerola.manga.local.database.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.relationship.Cover

@Dao
interface CoverDao : BaseDao<Cover> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Cover): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Cover): LongArray

    @Query(value = "SELECT * FROM cover WHERE mirror_id = :mirrorId LIMIT 1")
    suspend fun getCoverByMirrorId(mirrorId: String): Cover?
}