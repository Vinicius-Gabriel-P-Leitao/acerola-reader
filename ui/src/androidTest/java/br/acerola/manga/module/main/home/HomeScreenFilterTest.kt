package br.acerola.manga.module.main.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.home.state.FilterSettings
import br.acerola.manga.config.preference.HomeSortPreference
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import org.junit.Rule
import org.junit.Test

class HomeScreenFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayFilterSheetWhenClicked() {
        val sortSettings = HomeSortPreference(MangaSortType.TITLE, SortDirection.ASCENDING)
        val filterSettings = FilterSettings()

        composeTestRule.setContent {
            AcerolaTheme {
                Main.Home.Component.HomeFilterSheet(
                    sortSettings = sortSettings,
                    filterSettings = filterSettings,
                    categories = emptyList(),
                    onSortChange = {},
                    onFilterChange = {},
                    onDismiss = {}
                )
            }
        }

        // Verifica se o título da bottom sheet aparece
        composeTestRule.onNodeWithText("Filtrar e Ordenar").assertIsDisplayed()
        
        // Verifica se as seções principais aparecem
        composeTestRule.onNodeWithText("Ordenar por").assertIsDisplayed()
        composeTestRule.onNodeWithText("Filtrar por").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categorias").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fontes de Metadados").assertIsDisplayed()
    }

    @Test
    fun shouldShowAllSortingOptions() {
        composeTestRule.setContent {
            AcerolaTheme {
                Main.Home.Component.HomeFilterSheet(
                    sortSettings = HomeSortPreference(MangaSortType.TITLE, SortDirection.ASCENDING),
                    filterSettings = FilterSettings(),
                    categories = emptyList(),
                    onSortChange = {},
                    onFilterChange = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Título").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quantidade de capítulos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última atualização").assertIsDisplayed()
    }
}
