package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroNestedButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_title_and_description_of_nested_item() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedButton(
                    title = "Sync Chapters",
                    description = "Fetch remote chapters",
                    icon = Icons.Default.Sync,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Sync Chapters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fetch remote chapters").assertIsDisplayed()
    }

    @Test
    fun should_execute_click_callback_on_nested_item() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedButton(
                    title = "Clickable Item",
                    icon = Icons.Default.Sync,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Clickable Item").performClick()
        assertTrue(clicked)
    }
}
