package br.acerola.comic.module.comic

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.config.preference.ChapterSortPreferenceData
import br.acerola.comic.config.preference.ChapterSortType
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.module.comic.component.ChapterSortSheet
import org.junit.Rule
import org.junit.Test

class ComicScreenSortTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayChapterSortSheet() {
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
