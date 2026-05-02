package br.acerola.comic.module.main.config.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class SelectComicDirectoryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_configuracao_de_selecao_de_pasta_de_quadrinhos() {
        composeTestRule.setContent {
            Main.Config.Component.SelectComicDirectory(
                folderName = "Downloads/Manga",
                onFolderSelected = {},
            )
        }

        composeTestRule.onNodeWithText("Pasta dos quadrinhos", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pasta selecionada: Downloads/Manga").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("pasta", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
