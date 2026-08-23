package br.acerola.comic.module.main.home.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.config.preference.types.ComicSortType
import br.acerola.comic.config.preference.types.HomeSortPreference
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.home.state.FilterSettings
import org.junit.Rule
import org.junit.Test

class HomeFilterSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_filter_sheet_and_main_sections() {
        val sortSettings = HomeSortPreference(ComicSortType.TITLE, SortDirection.ASCENDING)
        val filterSettings = FilterSettings()

        composeTestRule.setContent {
            AcerolaTheme {
                Main.Home.Component.HomeFilterSheet(
                    sortSettings = sortSettings,
                    filterSettings = filterSettings,
                    categories = emptyList(),
                    onSortChange = {},
                    onFilterChange = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Filtrar e Ordenar", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ordenar por", substring = true).assertExists()
        composeTestRule.onNodeWithText("Categorias", substring = true).assertExists()
    }

    @Test
    fun should_display_comic_sorting_options() {
        composeTestRule.setContent {
            AcerolaTheme {
                Main.Home.Component.HomeFilterSheet(
                    sortSettings = HomeSortPreference(ComicSortType.TITLE, SortDirection.ASCENDING),
                    filterSettings = FilterSettings(),
                    categories = emptyList(),
                    onSortChange = {},
                    onFilterChange = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Título", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("capítulos", substring = true, ignoreCase = true).assertExists()
    }
}
