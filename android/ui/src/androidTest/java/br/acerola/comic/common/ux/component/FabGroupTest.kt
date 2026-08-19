package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class FabGroupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_group_items_when_expanding_main_fab() {
        var itemClicked = false
        val items =
            listOf(
                FabGroupItem(
                    label = "Action 1",
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { itemClicked = true },
                ),
            )

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.FabGroup(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Open") },
                    items = items,
                )
            }
        }

        composeTestRule.onNodeWithText("Action 1").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Open").performClick()
        composeTestRule.onNodeWithText("Action 1").assertIsDisplayed().performClick()

        assert(itemClicked)
    }
}
