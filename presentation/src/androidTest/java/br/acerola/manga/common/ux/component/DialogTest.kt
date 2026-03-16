package br.acerola.manga.common.ux.component

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_dialog_com_titulo_botoes_e_conteudo() {
        composeTestRule.setContent {
            Acerola.Component.Dialog(
                show = true,
                onDismiss = {},
                title = "Titulo Dialog",
                confirmButtonContent = { Button(onClick = {}) { Text("Confirmar") } },
                dismissButtonContent = { Button(onClick = {}) { Text("Cancelar") } }
            ) {
                Text("Conteudo Dialog")
            }
        }
        composeTestRule.onNodeWithText("Titulo Dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conteudo Dialog").assertIsDisplayed()
    }
}
