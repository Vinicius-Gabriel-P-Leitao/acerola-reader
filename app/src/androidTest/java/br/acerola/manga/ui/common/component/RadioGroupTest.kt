package br.acerola.manga.ui.common.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.ui.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class RadioGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRadioGroup_selectionChanges() {
        val options = listOf("Option 1", "Option 2", "Option 3")

        composeTestRule.setContent {
            val selectedIndex = remember { mutableStateOf(0) }
            AcerolaTheme {
                RadioGroup(
                    selectedIndex = selectedIndex.value,
                    options = options,
                    onSelect = { selectedIndex.value = it }
                )
            }
        }

        // Initial state
        composeTestRule.onNodeWithTag("radio_button_Option 1").assertIsSelected()

        // Click on another option's text (which is in a clickable Row)
        composeTestRule.onNodeWithText("Option 2").performClick()

        // Check if selection changed
        composeTestRule.onNodeWithTag("radio_button_Option 2").assertIsSelected()

        // Click on another option's radio button directly
        composeTestRule.onNodeWithTag("radio_button_Option 3").performClick()
        composeTestRule.onNodeWithTag("radio_button_Option 3").assertIsSelected()
    }
}
