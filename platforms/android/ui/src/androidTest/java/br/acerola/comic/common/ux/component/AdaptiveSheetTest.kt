package br.acerola.comic.common.ux.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class AdaptiveSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_content_inside_adaptive_sheet() {
        composeTestRule.setContent {
            Acerola.Component.AdaptiveSheet(
                onDismissRequest = {},
            ) {
                Text("Adaptive Content")
            }
        }

        composeTestRule.onNodeWithText("Adaptive Content").assertIsDisplayed()
    }
}
