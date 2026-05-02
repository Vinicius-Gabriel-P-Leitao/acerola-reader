package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.config.preference.types.ChapterSortPreferenceData
import br.acerola.comic.config.preference.types.ChapterSortType
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class ChapterSortSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_folha_de_ordenacao_de_capitulos() {
        val sortSettings = ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING)

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.ChapterSortSheet(
                    sortSettings = sortSettings,
                    onSortChange = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ordenar Capítulos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Número").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última atualização").assertIsDisplayed()
    }
}
