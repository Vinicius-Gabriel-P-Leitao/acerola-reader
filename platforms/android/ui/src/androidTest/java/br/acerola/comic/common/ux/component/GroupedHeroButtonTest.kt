package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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

class GroupedHeroButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_title_and_description_in_grouped_button() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Settings",
                    description = "Adjust preferences",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Adjust preferences").assertIsDisplayed()
    }

    @Test
    fun should_execute_click_callback_when_pressing_button() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
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
    fun should_display_nested_content_when_provided() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Main Item",
                    icon = Icons.Default.Settings,
                    nestedItem = {
                        Acerola.Component.HeroNestedButton(
                            title = "Nested Item",
                            icon = Icons.Default.Sync,
                            onClick = {},
                        )
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Nested Item").assertIsDisplayed()
    }
}
