package br.acerola.manga.local.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.metadata.relationship.Author

@Dao
interface AuthorDao : BaseDao<Author> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Author): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Author): LongArray

    @Query(value = "SELECT id FROM author WHERE name = :name AND manga_remote_info_fk = :mangaRemoteInfoFk LIMIT 1")
    suspend fun getIdByNameAndFk(name: String, mangaRemoteInfoFk: Long): Long?

    @Query(value = "DELETE FROM author WHERE manga_remote_info_fk = :mangaRemoteInfoFk")
    suspend fun deleteAuthorsByMangaRemoteInfoFk(mangaRemoteInfoFk: Long)

    @Transaction
    suspend fun insertOrGetId(entity: Author): Long {
        val id = insert(entity)

        return if (id != -1L) id
        else getIdByNameAndFk(entity.name, entity.mangaRemoteInfoFk)
            ?: throw IntegrityException(source = "Author", key = "name+fk")
    }
}
