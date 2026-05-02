package br.acerola.comic.common.ux.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class AdaptiveSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_conteudo_dentro_do_sheet_adaptativo() {
        composeTestRule.setContent {
            Acerola.Component.AdaptiveSheet(
                onDismissRequest = {},
            ) {
                Text("Conteudo Adaptativo")
            }
        }

        composeTestRule.onNodeWithText("Conteudo Adaptativo").assertIsDisplayed()
    }
}
