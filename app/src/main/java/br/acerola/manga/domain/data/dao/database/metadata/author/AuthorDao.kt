package br.acerola.manga.domain.data.dao.database.metadata.author

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.domain.data.dao.database.BaseDao
import br.acerola.manga.domain.model.metadata.author.Author

@Dao
interface AuthorDao : BaseDao<Author> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insert(entity: Author): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun insertAll(vararg entity: Author)

    @Query(value = "SELECT * FROM author WHERE mirror_id = :mirrorId LIMIT 1")
    suspend fun getAuthorByMirrorId(mirrorId: String): Author?
}