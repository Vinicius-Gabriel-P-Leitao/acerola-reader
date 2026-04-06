package br.acerola.comic.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.comic.error.exception.IntegrityException
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.relationship.Genre

@Dao
interface GenreDao : BaseDao<Genre> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Genre): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Genre): LongArray

    @Query(value = "SELECT id FROM genre WHERE genre = :genre AND comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getIdByGenreAndFk(
        genre: String,
        mangaRemoteInfoFk: Long
    ): Long?

    @Query(value = "DELETE FROM genre WHERE comic_metadata_fk = :mangaRemoteInfoFk")
    suspend fun deleteGenresByMangaRemoteInfoFk(mangaRemoteInfoFk: Long)

    @Transaction
    suspend fun insertOrGetId(entity: Genre): Long {
        val id = insert(entity)

        return if (id != -1L) id
        else getIdByGenreAndFk(entity.genre, entity.mangaRemoteInfoFk) ?: throw IntegrityException(
            source = "Genre", key = "genre+fk"
        )
    }
}
