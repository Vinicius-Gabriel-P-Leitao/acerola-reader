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
    fun should_display_available_categories_as_chips() {
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
    fun should_call_callback_when_selecting_a_category() {
        var selectedId: Long? = -1L
        val categories = listOf(CategoryDto(10L, "Test", 0xFFFFFF))

        composeTestRule.setContent {
            Comic.Component.ComicCategorySelector(
                selectedCategory = null,
                allCategories = categories,
                onUpdateMangaCategory = { selectedId = it },
            )
        }

        composeTestRule.onNodeWithText("Test").performClick()
        assert(selectedId == 10L)
    }
}
