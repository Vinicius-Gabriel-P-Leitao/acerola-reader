package br.acerola.manga.service.metadata

import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.local.dao.category.CategoryDao
import br.acerola.manga.local.entity.category.Category
import br.acerola.manga.local.entity.category.MangaCategory
import br.acerola.manga.local.translator.toCategoryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaCategoryService @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CategoryDto>> = categoryDao.getAllCategories().map { list ->
        list.map { it.toCategoryDto() }
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
                MangaCategory(
                    mangaDirectoryFk = directoryId, categoryId = categoryId
                )
            )
        }
    }

    fun getCategoryByMangaId(directoryId: Long): Flow<CategoryDto?> = 
        categoryDao.getCategoryByMangaId(directoryId).map { it?.toCategoryDto() }

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
