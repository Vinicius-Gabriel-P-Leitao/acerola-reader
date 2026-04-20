package br.acerola.comic.usecase.metadata

import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.service.metadata.CategoryManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageCategoriesUseCase
    @Inject
    constructor(
        private val categoryService: CategoryManager,
    ) {
        fun getAllCategories(): Flow<List<CategoryDto>> = categoryService.getAllCategories()

        suspend fun createCategory(
            name: String,
            color: Int,
        ) {
            categoryService.createCategory(name, color)
        }

        suspend fun deleteCategory(id: Long) {
            categoryService.deleteCategory(id)
        }

        suspend fun updateMangaCategory(
            directoryId: Long,
            categoryId: Long?,
        ) {
            categoryService.updateMangaCategory(directoryId, categoryId)
        }

        fun getCategoryByMangaId(directoryId: Long): Flow<CategoryDto?> = categoryService.getCategoryByMangaId(directoryId)

        fun getAllMangaCategories(): Flow<Map<Long, CategoryDto>> = categoryService.getAllMangaCategories()
    }
