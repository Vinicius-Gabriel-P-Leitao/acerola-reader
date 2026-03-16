package br.acerola.manga.module.main.config.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.module.main.Main
import org.junit.Rule
import org.junit.Test

class PreferSavedFileTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_extensao_de_arquivo_e_opcoes() {
        composeTestRule.setContent {
            Main.Config.Component.PreferSavedFile(
                selected = FileExtension.CBZ,
                onSelect = {}
            )
        }
        // Texto corrigido conforme strings.xml
        composeTestRule.onNodeWithText("Tipo do arquivo a ser salvo", substring = true).assertIsDisplayed()
        // Adicionado substring = true para encontrar ".cbz" e ".cbr"
        composeTestRule.onNodeWithText("cbz", ignoreCase = true, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("cbr", ignoreCase = true, substring = true).assertIsDisplayed()
    }
}
