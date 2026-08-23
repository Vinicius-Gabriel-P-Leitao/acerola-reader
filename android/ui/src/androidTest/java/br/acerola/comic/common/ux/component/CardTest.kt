package br.acerola.comic.common.ux.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class CardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_title_and_content() {
        composeTestRule.setContent {
            Acerola.Component.Card(title = "Test") {
                Text("Body")
            }
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Body").assertIsDisplayed()
    }
}
