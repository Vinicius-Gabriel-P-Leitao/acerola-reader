package br.acerola.manga.module.manga

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.config.preference.ChapterSortPreferenceData
import br.acerola.manga.config.preference.ChapterSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.module.manga.component.ChapterSortSheet
import org.junit.Rule
import org.junit.Test

class MangaScreenSortTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayChapterSortSheet() {
        val sortSettings = ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING)

        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Component.ChapterSortSheet(
                    sortSettings = sortSettings,
                    onSortChange = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Ordenar Capítulos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Número").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última atualização").assertIsDisplayed()
    }
}
