package br.acerola.manga.common.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class RadioGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `RadioGroup_deve_permitir_a_seleção_de_uma_opção_e_atualizar_o_estado_visual`() {
        val options = listOf("Opção 1", "Opção 2", "Opção 3")

        composeTestRule.setContent {
            var selectedIndex by remember { mutableIntStateOf(0) }
            RadioGroup(
                selectedIndex = selectedIndex,
                options = options,
                onSelect = { selectedIndex = it }
            )
        }

        // Verifica se a primeira está selecionada inicialmente
        composeTestRule.onNodeWithTag("radio_button_Opção 1").assertIsSelected()

        // Clica na segunda opção
        composeTestRule.onNodeWithText("Opção 2").performClick()

        // Verifica se a seleção mudou para a segunda opção
        composeTestRule.onNodeWithTag("radio_button_Opção 2").assertIsSelected()
    }
}
