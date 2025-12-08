package br.acerola.manga.domain.data.dao.database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<T : Any> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entity: T)

    @Update
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T)
}