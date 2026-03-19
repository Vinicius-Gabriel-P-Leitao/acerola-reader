package br.acerola.manga.module.main.config.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.module.main.Main
import org.junit.Rule
import org.junit.Test

class SyncLibraryArchiveTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_sincronizacao_e_botoes() {
        composeTestRule.setContent {
            Main.Config.Component.SyncLibraryArchive(
                onDeepScan = {},
                onQuickSync = {}
            )
        }
        // Usando o texto exato do strings.xml
        composeTestRule.onNodeWithText("Sincronizar", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronização rápida", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronização profunda", substring = true).assertIsDisplayed()
    }
}
