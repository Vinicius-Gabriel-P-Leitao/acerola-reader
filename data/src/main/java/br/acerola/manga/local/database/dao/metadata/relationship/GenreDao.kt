package br.acerola.manga.local.database.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.relationship.Genre

@Dao
interface GenreDao : BaseDao<Genre> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Genre): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Genre): LongArray

    @Query(value = "SELECT id FROM genre WHERE genre = :genre AND manga_remote_info_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getIdByGenreAndFk(
        genre: String,
        mangaRemoteInfoFk: Long
    ): Long?

    @Transaction
    suspend fun insertOrGetId(entity: Genre): Long {
        val id = insert(entity)

        return if (id != -1L) id
        else getIdByGenreAndFk(entity.genre, entity.mangaRemoteInfoFk) ?: throw IntegrityException(
            source = "Genre", key = "genre+fk"
        )
    }
}
