package br.acerola.manga.common.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SmartButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `botão_do_tipo_texto_deve_exibir_a_label_corretamente`() {
        var clicked = false
        composeTestRule.setContent {
            SmartButton(
                type = ButtonType.TEXT,
                text = "Clique Aqui",
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Clique Aqui").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun `botão_do_tipo_ícone_deve_exibir_a_descrição_de_conteúdo_corretamente`() {
        composeTestRule.setContent {
            SmartButton(
                type = ButtonType.ICON,
                onClick = {},
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar") }
            )
        }

        composeTestRule.onNodeWithContentDescription("Adicionar").assertIsDisplayed()
    }

    @Test
    fun `botão_do_tipo_misto_deve_exibir_ícone_e_texto_simultaneamente`() {
        composeTestRule.setContent {
            SmartButton(
                type = ButtonType.ICON_TEXT,
                text = "Salvar",
                onClick = {},
                icon = { Icon(Icons.Default.Add, contentDescription = "Icone") }
            )
        }

        composeTestRule.onNodeWithText("Salvar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Icone").assertIsDisplayed()
    }
}
