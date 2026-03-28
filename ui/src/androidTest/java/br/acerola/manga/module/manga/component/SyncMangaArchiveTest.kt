package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class SyncMangaArchiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_opcoes_de_sincronizacao_local() {
        composeTestRule.setContent {
            Manga.Component.SyncMangaArchive(
                onSyncChapters = {},
                onRescanCover = {},
                onExtractFirstPageAsCover = {}
            )
        }

        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizar cover e banner", substring = true).assertIsDisplayed()
    }

    @Test
    fun deve_chamar_onSyncChapters_ao_clicar_na_opcao_correspondente() {
        var clicked = false
        composeTestRule.setContent {
            Manga.Component.SyncMangaArchive(
                onSyncChapters = { clicked = true },
                onRescanCover = {},
                onExtractFirstPageAsCover = {}
            )
        }

        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).performClick()
        assert(clicked)
    }

    @Test
    fun deve_chamar_onRescanCover_ao_clicar_na_opcao_correspondente() {
        var clicked = false
        composeTestRule.setContent {
            Manga.Component.SyncMangaArchive(
                onSyncChapters = {},
                onRescanCover = { clicked = true },
                onExtractFirstPageAsCover = {}
            )
        }

        composeTestRule.onNodeWithText("Sincronizar cover e banner", substring = true).performClick()
        assert(clicked)
    }
}
