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
    fun `RadioGroup_deve_selecionar_a_opcao_corretamente_ao_clicar`() {
        var selectedIndex = 0
        val options = listOf("Opção A", "Opção B", "Opção C")

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.RadioGroup(
                    selectedIndex = selectedIndex,
                    options = options,
                    onSelect = { selectedIndex = it },
                )
            }
        }

        // Clica na \"Opção B\" (índice 1)
        composeTestRule.onNodeWithText("Opção B").performClick()

        assert(selectedIndex == 1)
    }
}
