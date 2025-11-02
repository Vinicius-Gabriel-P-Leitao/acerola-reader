
package br.acerola.manga.ui.common.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.ui.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class SmartButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testIconSmartButton_isDisplayed() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                SmartButton(
                    type = ButtonType.ICON,
                    onClick = { clicked = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Icon"
                        )
                    }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add Icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add Icon").performClick()
        assert(clicked)
    }

    @Test
    fun testTextSmartButton_isDisplayed() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                SmartButton(
                    type = ButtonType.TEXT,
                    onClick = { clicked = true },
                    text = "Click Me"
                )
            }
        }

        composeTestRule.onNodeWithText("Click Me").assertIsDisplayed()
        composeTestRule.onNodeWithText("Click Me").performClick()
        assert(clicked)
    }

    @Test
    fun testIconTextSmartButton_isDisplayed() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                SmartButton(
                    type = ButtonType.ICON_TEXT,
                    onClick = { clicked = true },
                    text = "Click Me",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Icon"
                        )
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Click Me").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add Icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Click Me").performClick()
        assert(clicked)
    }
}
