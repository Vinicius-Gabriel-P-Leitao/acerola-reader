package br.acerola.manga.local.database.dao.metadata.relationship

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.local.database.dao.BaseDao
import br.acerola.manga.local.database.entity.metadata.relationship.Author

@Dao
interface AuthorDao : BaseDao<Author> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Author): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Author): LongArray

    @Query(value = "SELECT id FROM author WHERE mirror_id = :mirrorId LIMIT 1")
    suspend fun getIdByMirrorId(mirrorId: String): Long?

    @Transaction
    suspend fun insertOrGetId(entity: Author): Long {
        val id = insert(entity)

        return if (id != -1L) id
        else getIdByMirrorId(entity.mirrorId) ?: throw IntegrityException(source = "Author", key = "mirrorId")
    }
}