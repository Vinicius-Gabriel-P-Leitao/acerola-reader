package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.__fixtures__.MangaFixtures
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.state.MangaUiState
import org.junit.Rule
import org.junit.Test

class ConfigSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve exibir todas as secoes de configuracao do manga`() {
        val uiState = MangaUiState(
            manga = MangaDto(
                directory = MangaFixtures.createMangaDirectoryDto(),
                remoteInfo = null
            ),
            selectedChapterPerPage = ChapterPageSizeType.SHORT
        )

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
        composeTestRule.onNodeWithText("Sincronizar metadados", substring = true).assertIsDisplayed()
    }
}
