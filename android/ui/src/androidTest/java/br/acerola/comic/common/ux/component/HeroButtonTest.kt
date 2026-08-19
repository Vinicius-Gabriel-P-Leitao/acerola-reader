package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_title_and_description_correctly() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Settings",
                    description = "Item description",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item description").assertIsDisplayed()
    }

    @Test
    fun should_execute_click_callback() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Clickable Item",
                    icon = Icons.Default.Settings,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Clickable Item").performClick()
        assertTrue(clicked)
    }

    @Test
    fun should_render_side_action_slot() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Item with Switch",
                    icon = Icons.Default.Settings,
                    action = { Switch(checked = false, onCheckedChange = null) },
                )
            }
        }

        composeTestRule.onNodeWithText("Item with Switch").assertIsDisplayed()
    }

    @Test
    fun should_display_bottom_content_when_configured() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Item with Extra",
                    icon = Icons.Default.Settings,
                    bottomContent = { Text("Bottom Content") },
                )
            }
        }

        composeTestRule.onNodeWithText("Bottom Content").assertIsDisplayed()
    }
}
