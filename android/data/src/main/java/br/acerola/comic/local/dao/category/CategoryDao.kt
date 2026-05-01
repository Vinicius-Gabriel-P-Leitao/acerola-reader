package br.acerola.comic.local.dao.category

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.category.Category
import br.acerola.comic.local.entity.category.ComicCategory
import br.acerola.comic.local.entity.relation.ComicCategoryJoinResult
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao : BaseDao<Category> {
    @Query("SELECT * FROM category ORDER BY name ASC")
    fun observeAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicCategory(comicCategory: ComicCategory)

    @Query("DELETE FROM comic_category WHERE comic_directory_fk = :comicId")
    suspend fun deleteComicCategoryByDirectoryId(comicId: Long)

    @Query(
        """
        SELECT category.* FROM category
        INNER JOIN comic_category ON category.id = comic_category.category_id
        WHERE comic_category.comic_directory_fk = :comicId
        """,
    )
    fun observeCategoryByDirectoryId(comicId: Long): Flow<Category?>

    @Query(
        """
        SELECT comic_category.comic_directory_fk, category.* FROM category
        INNER JOIN comic_category ON category.id = comic_category.category_id
        """,
    )
    fun observeAllComicCategoriesJoined(): Flow<List<ComicCategoryJoinResult>>
}
