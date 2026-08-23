package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.config.preference.types.ChapterPageSizeType
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class PaginationPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_title_and_pagination_options() {
        composeTestRule.setContent {
            Comic.Component.PaginationPreference(
                selected = ChapterPageSizeType.SHORT,
                onSelect = {},
            )
        }

        composeTestRule.onNodeWithText("Capítulos pré-carregados", substring = true).assertIsDisplayed()
        // "25" aparece duas vezes: na descrição do HeroItem (selecionado) e no RadioGroup
        composeTestRule.onAllNodesWithText("25", ignoreCase = true)[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("50", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("100", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun should_call_onSelect_when_clicking_an_option() {
        var selectedSize: ChapterPageSizeType? = null
        composeTestRule.setContent {
            Comic.Component.PaginationPreference(
                selected = ChapterPageSizeType.SHORT,
                onSelect = { selectedSize = it },
            )
        }

        composeTestRule.onNodeWithText("50").performClick()
        assert(selectedSize == ChapterPageSizeType.MEDIUM)
    }
}
