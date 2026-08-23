package br.acerola.comic.module.reader.template

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class BottomControlsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderBottomControls_should_display_current_page_number_correctly`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.BottomControls(
                    pageCount = 50,
                    currentPage = 9, // Página 10
                    onPrevClick = {},
                    onNextClick = {},
                    onNextChapterClick = {},
                    onPreviousChapterClick = {},
                )
            }
        }

        // Verifica se a string "10 / 50" está presente
        composeTestRule.onNodeWithText("10 / 50").assertIsDisplayed()
    }

    @Test
    fun `next_chapter_button_should_appear_only_when_chapter_is_read_and_has_next`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.BottomControls(
                    pageCount = 10,
                    currentPage = 9,
                    isChapterRead = true,
                    hasNextChapter = true,
                    onPrevClick = {},
                    onNextClick = {},
                    onNextChapterClick = {},
                    onPreviousChapterClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Próximo", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `previous_chapter_button_should_appear_when_has_previous`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.BottomControls(
                    pageCount = 10,
                    currentPage = 0,
                    hasPreviousChapter = true,
                    onPrevClick = {},
                    onNextClick = {},
                    onNextChapterClick = {},
                    onPreviousChapterClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Anterior", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
