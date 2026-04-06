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
    fun deve_exibir_titulo_e_conteudo() {
        composeTestRule.setContent {
            Acerola.Component.Card(title = "Teste") {
                Text("Corpo")
            }
        }
        composeTestRule.onNodeWithText("Teste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Corpo").assertIsDisplayed()
    }
}
