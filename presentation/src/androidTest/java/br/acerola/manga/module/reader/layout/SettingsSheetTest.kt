package br.acerola.manga.module.reader.layout

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class SettingsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `deve exibir opcoes de modo de leitura`() {
        composeTestRule.setContent {
            Reader.Layout.SettingsSheet(
                currentMode = ReadingMode.HORIZONTAL,
                onModeChange = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Configurações de Leitura", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Horizontal", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Vertical", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Webtoon", substring = true).assertIsDisplayed()
    }
}
