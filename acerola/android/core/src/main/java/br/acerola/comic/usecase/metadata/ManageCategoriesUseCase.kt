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

        suspend fun updateComicCategory(
            directoryId: Long,
            categoryId: Long?,
        ) {
            categoryService.updateComicCategory(directoryId, categoryId)
        }

        fun getCategoryByComicId(directoryId: Long): Flow<CategoryDto?> = categoryService.getCategoryByComicId(directoryId)

        fun getAllComicCategories(): Flow<Map<Long, CategoryDto>> = categoryService.getAllComicCategories()
    }
