package br.acerola.comic.module.comic.template

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.fixtures.ComicFixtures
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class ConfigSectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_all_comic_config_sections() {
        val uiState = ComicFixtures.createMangaUiState()

        composeTestRule.setContent {
            LazyColumn {
                Comic.Template.configSection(
                    scope = this,
                    uiState = uiState,
                    onAction = {},
                    onSyncAction = {},
                )
            }
        }

        // Títulos das seções usam SectionHeader que aplica uppercase
        composeTestRule.onNodeWithText("Configurações de Exibição", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Configuração dos arquivos", ignoreCase = true).assertIsDisplayed()

        // MangaDex é exibido dentro do componente SyncMetadata
        composeTestRule.onNodeWithText("MangaDex", ignoreCase = true).assertIsDisplayed()
    }
}
