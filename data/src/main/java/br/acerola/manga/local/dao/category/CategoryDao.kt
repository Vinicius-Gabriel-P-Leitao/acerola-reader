package br.acerola.manga.local.dao.category

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.entity.category.Category
import br.acerola.manga.local.entity.category.MangaCategory
import br.acerola.manga.local.entity.relation.MangaCategoryJoinResult
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao : BaseDao<Category> {

    @Query("SELECT * FROM category ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaCategory(mangaCategory: MangaCategory)

    @Query("DELETE FROM manga_category WHERE manga_directory_fk = :mangaId")
    suspend fun deleteMangaCategory(mangaId: Long)

    @Query("""
        SELECT category.* FROM category
        INNER JOIN manga_category ON category.id = manga_category.category_id
        WHERE manga_category.manga_directory_fk = :mangaId
    """)
    fun getCategoryByMangaId(mangaId: Long): Flow<Category?>

    @Query("""
        SELECT manga_category.manga_directory_fk, category.* FROM category
        INNER JOIN manga_category ON category.id = manga_category.category_id
    """)
    fun getAllMangaCategoriesJoined(): Flow<List<MangaCategoryJoinResult>>
}
