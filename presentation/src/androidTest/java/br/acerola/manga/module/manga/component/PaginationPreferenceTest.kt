package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.MangaViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class PaginationPreferenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve exibir titulo e opcoes de paginacao`() {
        composeTestRule.setContent {
            Manga.Component.PaginationPreference(
                selected = ChapterPageSizeType.SHORT,
                onSelect = {}
            )
        }

        composeTestRule.onNodeWithText("Capítulos por página", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("20", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("50", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("100", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `deve chamar onSelect ao clicar em uma opcao`() {
        var selectedSize: ChapterPageSizeType? = null
        composeTestRule.setContent {
            Manga.Component.PaginationPreference(
                selected = ChapterPageSizeType.SHORT,
                onSelect = { selectedSize = it }
            )
        }

        composeTestRule.onNodeWithText("50").performClick()
        assert(selectedSize == ChapterPageSizeType.MEDIUM)
    }
}
