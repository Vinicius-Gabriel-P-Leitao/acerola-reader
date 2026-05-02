package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class ComicCategorySelectorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_categorias_disponiveis_como_chips() {
        val categories =
            listOf(
                CategoryDto(1L, "Shonen", 0xFF0000),
                CategoryDto(2L, "Seinen", 0x0000FF),
            )

        composeTestRule.setContent {
            Comic.Component.ComicCategorySelector(
                selectedCategory = null,
                allCategories = categories,
                onUpdateMangaCategory = {},
            )
        }

        composeTestRule.onNodeWithText("Shonen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seinen").assertIsDisplayed()
    }

    @Test
    fun deve_chamar_callback_ao_selecionar_uma_categoria() {
        var selectedId: Long? = -1L
        val categories = listOf(CategoryDto(10L, "Teste", 0xFFFFFF))

        composeTestRule.setContent {
            Comic.Component.ComicCategorySelector(
                selectedCategory = null,
                allCategories = categories,
                onUpdateMangaCategory = { selectedId = it },
            )
        }

        composeTestRule.onNodeWithText("Teste").performClick()
        assert(selectedId == 10L)
    }
}
