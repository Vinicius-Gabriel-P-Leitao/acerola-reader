package br.acerola.comic.common.ux.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class RadioGroupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `RadioGroup_should_select_option_correctly_when_clicked`() {
        var selectedIndex = 0
        val options = listOf("Option A", "Option B", "Option C")

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.RadioGroup(
                    selectedIndex = selectedIndex,
                    options = options,
                    onSelect = { selectedIndex = it },
                )
            }
        }

        // Clica na "Option B" (índice 1)
        composeTestRule.onNodeWithText("Option B").performClick()

        assert(selectedIndex == 1)
    }
}
