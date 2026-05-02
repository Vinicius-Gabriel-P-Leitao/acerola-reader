package br.acerola.comic.module.main.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.config.preference.types.ComicSortType
import br.acerola.comic.config.preference.types.HomeLayoutType
import br.acerola.comic.config.preference.types.HomeSortPreference
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.error.UserMessage
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.home.state.FilterSettings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HomeViewModel>(relaxed = true)

    @Before
    fun setUp() {
        every { viewModel.selectedHomeLayout } returns MutableStateFlow(HomeLayoutType.LIST)
        every { viewModel.isIndexing } returns MutableStateFlow(false)
        every { viewModel.progress } returns MutableStateFlow(-1)
        every { viewModel.comics } returns MutableStateFlow(emptyList())
        every { viewModel.uiEvents } returns MutableSharedFlow<UserMessage>().asSharedFlow()
        every { viewModel.allCategories } returns MutableStateFlow(emptyList())

        // Mock com objetos reais, não proxies
        every { viewModel.sortSettings } returns MutableStateFlow(HomeSortPreference(ComicSortType.TITLE, SortDirection.ASCENDING))
        every { viewModel.filterSettings } returns MutableStateFlow(FilterSettings())
    }

    @Test
    fun `HomeScreen_deve_exibir_SearchBar_quando_carregada`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.Home.Template.Screen(homeViewModel = viewModel, onNavigateToConfig = {})
                }
            }
        }

        // Verifica se o placeholder da busca aparece
        composeTestRule.onNodeWithText("Buscar quadrinho...", substring = true).assertIsDisplayed()

        // Verifica se o FloatingTool (HUB) está presente
        composeTestRule.onNodeWithContentDescription("hub", substring = true).assertIsDisplayed()
    }
}
