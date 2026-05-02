package br.acerola.comic.usecase.metadata

import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.service.metadata.CategoryManager
import io.mockk.MockKAnnotations
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ManageCategoriesUseCaseTest {
    @MockK
    lateinit var categoryManager: CategoryManager

    private lateinit var useCase: ManageCategoriesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = ManageCategoriesUseCase(categoryService = categoryManager)
    }

    // getAllCategories — observa lista
    @Test
    fun `getAllCategories deve retornar o fluxo do manager exatamente uma vez`() =
        runTest {
            val categories = listOf(mockk<CategoryDto>())
            every { categoryManager.getAllCategories() } returns MutableStateFlow(categories)

            val result = useCase.getAllCategories().first()

            assertEquals(categories, result)
            coVerify(exactly = 1) { categoryManager.getAllCategories() }
        }

    // createCategory — delega ao manager
    @Test
    fun `createCategory deve chamar o manager exatamente uma vez`() =
        runTest {
            coJustRun { categoryManager.createCategory(any(), any()) }

            useCase.createCategory(name = "Favoritos", color = 0xFF0000)

            coVerify(exactly = 1) { categoryManager.createCategory("Favoritos", 0xFF0000) }
        }

    // deleteCategory — delega ao manager
    @Test
    fun `deleteCategory deve chamar o manager exatamente uma vez`() =
        runTest {
            coJustRun { categoryManager.deleteCategory(any()) }

            useCase.deleteCategory(id = 7L)

            coVerify(exactly = 1) { categoryManager.deleteCategory(7L) }
        }

    // updateComicCategory — associa categoria a comic
    @Test
    fun `updateComicCategory deve chamar o manager exatamente uma vez`() =
        runTest {
            coJustRun { categoryManager.updateComicCategory(any(), any()) }

            useCase.updateComicCategory(directoryId = 10L, categoryId = 3L)

            coVerify(exactly = 1) { categoryManager.updateComicCategory(10L, 3L) }
        }

    // updateComicCategory — desassocia quando categoryId é null
    @Test
    fun `updateComicCategory com categoryId null deve chamar o manager com null`() =
        runTest {
            coJustRun { categoryManager.updateComicCategory(any(), isNull()) }

            useCase.updateComicCategory(directoryId = 10L, categoryId = null)

            coVerify(exactly = 1) { categoryManager.updateComicCategory(10L, null) }
        }
}
