package br.acerola.manga.core.usecase.metadata

import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.service.metadata.MangaCategoryService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageCategoriesUseCase @Inject constructor(
    private val categoryService: MangaCategoryService
) {
    fun getAllCategories(): Flow<List<CategoryDto>> = categoryService.getAllCategories()

    suspend fun createCategory(name: String, color: Int) {
        categoryService.createCategory(name, color)
    }

    suspend fun deleteCategory(id: Long) {
        categoryService.deleteCategory(id)
    }

    suspend fun updateMangaCategory(directoryId: Long, categoryId: Long?) {
        categoryService.updateMangaCategory(directoryId, categoryId)
    }

    fun getCategoryByMangaId(directoryId: Long): Flow<CategoryDto?> = 
        categoryService.getCategoryByMangaId(directoryId)

    fun getAllMangaCategories(): Flow<Map<Long, CategoryDto>> = 
        categoryService.getAllMangaCategories()
}
