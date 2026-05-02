package br.acerola.comic.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.comic.error.exception.IntegrityException
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.relationship.Genre

@Dao
interface GenreDao : BaseDao<Genre> {
    @Query(value = "SELECT id FROM genre WHERE genre = :genre AND comic_metadata_fk = :comicRemoteInfoFk LIMIT 1")
    suspend fun getIdByNameAndMetadataId(
        genre: String,
        comicRemoteInfoFk: Long,
    ): Long?

    @Query(value = "DELETE FROM genre WHERE comic_metadata_fk = :comicRemoteInfoFk")
    suspend fun deleteByMetadataId(comicRemoteInfoFk: Long)

    @Transaction
    suspend fun upsertAndGetId(entity: Genre): Long {
        val id = insert(entity)

        return if (id != -1L) {
            id
        } else {
            getIdByNameAndMetadataId(entity.genre, entity.comicRemoteInfoFk) ?: throw IntegrityException(
                source = "Genre",
                key = "genre+fk",
            )
        }
    }
}
