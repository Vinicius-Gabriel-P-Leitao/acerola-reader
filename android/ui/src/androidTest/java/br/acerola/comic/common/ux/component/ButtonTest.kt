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
import org.junit.Rule
import org.junit.Test

class ButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_renderizar_botao_com_label_e_executar_clique() {
        var clicked = false
        composeTestRule.setContent {
            Acerola.Component.Button(
                text = "Clique Aqui",
                onClick = { clicked = true },
            )
        }

        composeTestRule.onNodeWithText("Clique Aqui").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun deve_renderizar_icon_button_com_content_description() {
        composeTestRule.setContent {
            Acerola.Component.IconButton(
                onClick = {},
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar") },
            )
        }

        composeTestRule.onNodeWithContentDescription("Adicionar").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_icone_e_texto_no_botao_misto() {
        composeTestRule.setContent {
            Acerola.Component.Button(
                text = "Salvar",
                onClick = {},
                icon = { Icon(Icons.Default.Add, contentDescription = "Icone") },
            )
        }

        composeTestRule.onNodeWithText("Salvar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Icone").assertIsDisplayed()
    }
}
