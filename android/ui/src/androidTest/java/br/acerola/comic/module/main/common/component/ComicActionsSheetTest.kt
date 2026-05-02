package br.acerola.comic.module.main.common.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.comic.fixtures.ComicFixtures
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R
import org.junit.Rule
import org.junit.Test

class ComicActionsSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deve_exibir_acoes_principais_ao_abrir_o_sheet() {
        val comic = ComicFixtures.createMangaUiState().comic

        composeTestRule.setContent {
            Main.Common.Component.ComicActionsSheet(
                comic = comic,
                categories = emptyList(),
                onHide = {},
                onDelete = {},
                onBookmark = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.action_bookmark)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_hide)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_delete)).assertIsDisplayed()
    }

    @Test
    fun deve_exibir_dialogo_de_confirmacao_ao_clicar_em_ocultar() {
        composeTestRule.setContent {
            Main.Common.Component.ComicActionsSheet(
                comic = ComicFixtures.createMangaUiState().comic,
                categories = emptyList(),
                onHide = {},
                onDelete = {},
                onBookmark = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.action_hide)).performClick()

        // Verifica se o título do diálogo de ocultar aparece
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_hide_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_cancel)).assertIsDisplayed()
    }

    @Test
    fun deve_exibir_dialogo_de_confirmacao_ao_clicar_em_deletar() {
        composeTestRule.setContent {
            Main.Common.Component.ComicActionsSheet(
                comic = ComicFixtures.createMangaUiState().comic,
                categories = emptyList(),
                onHide = {},
                onDelete = {},
                onBookmark = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.action_delete)).performClick()

        // Verifica se o título do diálogo de deletar aparece
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_delete_title)).assertIsDisplayed()
    }
}
