package br.acerola.manga.module.main.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.home.state.FilterSettings
import br.acerola.manga.config.preference.HomeSortPreference
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.module.main.home.component.HomeFilterSheet
import br.acerola.manga.dto.metadata.category.CategoryDto
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
                    categories = emptyList<CategoryDto>(),
                    onSortChange = {},
                    onFilterChange = {},
                    onDismiss = {}
                )
            }
        }

        // Verifica se o título da bottom sheet aparece (correspondência exata para evitar conflito com cabeçalhos de seção)
        composeTestRule.onNodeWithText("Filtrar e Ordenar").assertIsDisplayed()
        
        // Verifica se as seções principais aparecem
        composeTestRule.onNodeWithText("Ordenar por", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Categorias", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Fontes", substring = true).assertIsDisplayed()
    }

    @Test
    fun shouldShowAllSortingOptions() {
        composeTestRule.setContent {
            AcerolaTheme {
                Main.Home.Component.HomeFilterSheet(
                    sortSettings = HomeSortPreference(MangaSortType.TITLE, SortDirection.ASCENDING),
                    filterSettings = FilterSettings(),
                    categories = emptyList<CategoryDto>(),
                    onSortChange = {},
                    onFilterChange = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Título", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("capítulos", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("atualização", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }
}
