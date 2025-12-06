package br.acerola.manga.domain.database.dao.database.metadata.gender

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.domain.database.dao.database.BaseDao
import br.acerola.manga.domain.model.metadata.gender.Gender
import kotlinx.coroutines.flow.Flow

@Dao
interface GenderDao : BaseDao<Gender> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Gender): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Gender)

    @Query(value = "SELECT * FROM gender WHERE mirror_id = :mirrorId LIMIT 1")
    suspend fun getGenderByMirrorId(mirrorId: String): Gender?
}