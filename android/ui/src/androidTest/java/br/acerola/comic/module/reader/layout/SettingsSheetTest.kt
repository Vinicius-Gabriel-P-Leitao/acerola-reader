package br.acerola.comic.module.reader.layout

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class SettingsSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun deve_exibir_opcoes_de_modo_de_leitura() {
        composeTestRule.setContent {
            Reader.Layout.SettingsSheet(
                onDismissRequest = {},
                currentMode = ReadingMode.HORIZONTAL,
                onModeSelected = {},
            )
        }

        // Os textos devem coincidir com strings.xml (Paginado para Horizontal)
        composeTestRule.onNodeWithText("Layout de Leitura", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Paginado", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Vertical", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Webtoon", substring = true, useUnmergedTree = true).assertExists()
    }
}
