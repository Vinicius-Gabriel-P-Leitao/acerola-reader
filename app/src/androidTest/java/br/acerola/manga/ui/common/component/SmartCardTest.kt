
package br.acerola.manga.ui.common.component

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.ui.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class SmartCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testImageSmartCard_isDisplayed() {
        composeTestRule.setContent {
            AcerolaTheme {
                SmartCard(
                    type = CardType.IMAGE,
                    title = "Image Card",
                    footer = "Image Footer",
                    image = ColorPainter(Color.Red)
                )
            }
        }

        composeTestRule.onNodeWithText("Image Footer").assertIsDisplayed()
    }

    @Test
    fun testTextSmartCard_isDisplayed() {
        composeTestRule.setContent {
            AcerolaTheme {
                SmartCard(
                    type = CardType.TEXT,
                    title = "Text Card",
                    text = "This is a text card.",
                    footer = "Text Footer"
                )
            }
        }

        composeTestRule.onNodeWithText("Text Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a text card.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text Footer").assertIsDisplayed()
    }

    @Test
    fun testContentSmartCard_isDisplayed() {
        composeTestRule.setContent {
            AcerolaTheme {
                SmartCard(
                    type = CardType.CONTENT,
                    title = "Content Card",
                    footer = "Content Footer",
                    content = {
                        Text("This is a content card.")
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Content Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a content card.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content Footer").assertIsDisplayed()
    }
}
