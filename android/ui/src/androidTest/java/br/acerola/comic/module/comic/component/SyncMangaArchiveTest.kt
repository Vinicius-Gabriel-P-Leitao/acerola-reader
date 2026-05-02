package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class SyncMangaArchiveTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_opcoes_de_sincronizacao_de_arquivos_do_manga() {
        composeTestRule.setContent {
            Comic.Component.SyncMangaArchive(
                onSyncChapters = {},
                onRescanCover = {},
                onExtractFirstPageAsCover = {},
            )
        }

        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizar cover e banner", substring = true).assertIsDisplayed()
    }

    @Test
    fun deve_chamar_onSyncChapters_ao_clicar_na_opcao() {
        var clicked = false
        composeTestRule.setContent {
            Comic.Component.SyncMangaArchive(
                onSyncChapters = { clicked = true },
                onRescanCover = {},
                onExtractFirstPageAsCover = {},
            )
        }

        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).performClick()
        assert(clicked)
    }
}
