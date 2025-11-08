package br.acerola.manga.ui.common.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import br.acerola.manga.ui.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class FloatingToolTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFloatingTool_expandsAndCollapses() {
        var settingsClicked = false
        var favoriteClicked = false

        val items = listOf(
            FloatingToolItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                onClick = { settingsClicked = true }
            ),
            FloatingToolItem(
                icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorite") },
                onClick = { favoriteClicked = true }
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                FloatingTool(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    items = items
                )
            }
        }

        // Initially, only the main button is visible
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Favorite").assertDoesNotExist()

        // Click to expand
        composeTestRule.onNodeWithContentDescription("Add").performClick()

        // Items should be visible now
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Favorite").assertIsDisplayed()

        // Click on an item
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assert(settingsClicked)
        assert(!favoriteClicked)

        // After clicking an item, the menu should collapse
        composeTestRule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Favorite").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()

        // Expand again
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Favorite").assertIsDisplayed()

        // Click main button to collapse
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Favorite").assertDoesNotExist()
    }
}
