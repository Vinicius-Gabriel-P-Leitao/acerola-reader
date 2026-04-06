package br.acerola.comic.service.metadata

import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.local.dao.category.CategoryDao
import br.acerola.comic.local.entity.category.Category
import br.acerola.comic.local.entity.category.ComicCategory
import br.acerola.comic.local.translator.ui.toViewDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryManager @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CategoryDto>> = categoryDao.getAllCategories().map { list ->
        list.map { it.toViewDto() }
    }

    suspend fun createCategory(name: String, color: Int) {
        categoryDao.insert(Category(name = name, color = color))
    }

    suspend fun deleteCategory(id: Long) {
        categoryDao.delete(Category(id = id, name = "", color = 0))
    }

    suspend fun updateMangaCategory(directoryId: Long, categoryId: Long?) {
        categoryDao.deleteMangaCategory(directoryId)
        if (categoryId != null) {
            categoryDao.insertMangaCategory(
                ComicCategory(
                    mangaDirectoryFk = directoryId, categoryId = categoryId
                )
            )
        }
    }

    fun getCategoryByMangaId(directoryId: Long): Flow<CategoryDto?> = 
        categoryDao.getCategoryByMangaId(directoryId).map { it?.toViewDto() }

    fun getAllMangaCategories(): Flow<Map<Long, CategoryDto>> = 
        categoryDao.getAllMangaCategoriesJoined().map { list ->
            list.associate { result ->
                result.mangaDirectoryId to CategoryDto(
                    id = result.categoryId, 
                    name = result.name, 
                    color = result.color
                )
            }
        }
}
