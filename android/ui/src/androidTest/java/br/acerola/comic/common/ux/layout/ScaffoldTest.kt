package br.acerola.comic.common.ux.layout

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class ScaffoldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_conteudo() {
        composeTestRule.setContent {
            Acerola.Layout.Scaffold {
                Text("Conteúdo")
            }
        }
        composeTestRule.onNodeWithText("Conteúdo").assertIsDisplayed()
    }
}
