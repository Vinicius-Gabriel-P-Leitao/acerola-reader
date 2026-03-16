package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.__fixtures__.MangaFixtures
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class ConfigSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_todas_as_secoes_de_configuracao_do_manga() {
        val uiState = MangaFixtures.createMangaUiState()

        composeTestRule.setContent {
            LazyColumn {
                Manga.Layout.ConfigSection(
                    scope = this,
                    uiState = uiState,
                    onAction = {},
                    onSyncAction = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Configurações de Exibição", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Configuração dos arquivos", substring = true).assertIsDisplayed()
        // Usa o texto completo para evitar ambiguidade com o item interno "Sincronizar metadados"
        composeTestRule.onNodeWithText("Sincronizar metadados com mangadex", substring = true).assertIsDisplayed()
    }
}
