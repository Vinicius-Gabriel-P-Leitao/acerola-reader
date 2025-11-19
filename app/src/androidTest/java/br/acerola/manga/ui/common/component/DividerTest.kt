package br.acerola.manga.ui.common.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import br.acerola.manga.ui.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class DividerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDivider_isDisplayed() {
        composeTestRule.setContent {
            AcerolaTheme {
                Divider(modifier = Modifier.testTag("divider"))
            }
        }

        composeTestRule.onNodeWithTag("divider").assertIsDisplayed()
    }
}
