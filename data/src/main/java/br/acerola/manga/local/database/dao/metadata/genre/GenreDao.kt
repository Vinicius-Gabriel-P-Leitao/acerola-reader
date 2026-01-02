package br.acerola.manga.local.database.dao.metadata.genre

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.relationship.Genre

@Dao
interface GenreDao : BaseDao<Genre> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Genre): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Genre)

    @Query(value = "SELECT * FROM genre WHERE mirror_id = :mirrorId LIMIT 1")
    suspend fun getGenreByMirrorId(mirrorId: String): Genre?
}