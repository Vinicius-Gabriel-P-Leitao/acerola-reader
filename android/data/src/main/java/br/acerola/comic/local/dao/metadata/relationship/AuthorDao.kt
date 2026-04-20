package br.acerola.comic.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.comic.error.exception.IntegrityException
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.metadata.relationship.Author

@Dao
interface AuthorDao : BaseDao<Author> {
    @Query(value = "SELECT id FROM author WHERE name = :name AND comic_metadata_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getIdByNameAndMetadataId(
        name: String,
        mangaRemoteInfoFk: Long,
    ): Long?

    @Query(value = "DELETE FROM author WHERE comic_metadata_fk = :mangaRemoteInfoFk")
    suspend fun deleteByMetadataId(mangaRemoteInfoFk: Long)

    @Transaction
    suspend fun upsertAndGetId(entity: Author): Long {
        val id = insert(entity)

        return if (id != -1L) {
            id
        } else {
            getIdByNameAndMetadataId(entity.name, entity.mangaRemoteInfoFk)
                ?: throw IntegrityException(source = "Author", key = "name+fk")
        }
    }
}
