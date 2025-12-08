package br.acerola.manga.domain.data.dao.database.metadata.cover

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.domain.data.dao.database.BaseDao
import br.acerola.manga.domain.model.metadata.cover.Cover

@Dao
interface CoverDao : BaseDao<Cover> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Cover): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Cover)

    @Query(value = "SELECT * FROM cover WHERE mirror_id = :mirrorId LIMIT 1")
    suspend fun getCoverByMirrorId(mirrorId: String): Cover?
}