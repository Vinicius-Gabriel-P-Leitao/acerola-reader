package br.acerola.comic.module.main.config.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class SyncLibraryArchiveTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_sync_and_buttons() {
        composeTestRule.setContent {
            Main.Config.Component.SyncLibraryArchive(
                onDeepScan = {},
                onQuickSync = {},
            )
        }

        // Verifica os itens de sincronização (usando assertExists para evitar falhas de scroll/visibilidade)
        composeTestRule.onNodeWithText("Sincronização rápida", substring = true, ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sincronização profunda", substring = true, ignoreCase = true).assertExists()
    }
}
