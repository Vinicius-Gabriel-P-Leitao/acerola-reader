package br.acerola.comic.module.reader.template

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class TopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderTopBar_should_display_title_and_subtitle_correctly`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.TopBar(
                    title = "Solo Leveling",
                    subtitle = "Chapter 150",
                    isVisible = true,
                    onBackClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Solo Leveling").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chapter 150").assertIsDisplayed()
    }

    @Test
    fun `ReaderTopBar_should_be_hidden_when_isVisible_is_false`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.TopBar(
                    title = "Anything",
                    subtitle = "Something",
                    isVisible = false,
                    onBackClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Anything").assertDoesNotExist()
    }
}
