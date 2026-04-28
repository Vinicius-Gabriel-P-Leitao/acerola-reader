package br.acerola.comic.module.main.config.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class SelectFolderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_pasta_da_biblioteca_e_botao_acesso() {
        composeTestRule.setContent {
            Main.Config.Component.SelectComicDirectory(
                folderName = null,
                onFolderSelected = {},
            )
        }
        // Texto corrigido conforme strings.xml
        composeTestRule.onNodeWithText("Pasta dos mangás", substring = true).assertIsDisplayed()
    }
}
