package br.acerola.comic.module.main.config.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class SyncMangadexDataTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_sync_mangadex_metadata() {
        composeTestRule.setContent {
            Main.Config.Component.SyncMangadexData(
                onRescan = {},
            )
        }
        // No SyncMangadexData.kt o texto é "Sincronizar metadados"
        composeTestRule.onNodeWithText("Sincronizar metadados", substring = true).assertIsDisplayed()
    }
}
