package br.acerola.manga.module.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.home.HomeViewModel
import br.acerola.manga.module.main.home.Screen
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HomeViewModel>(relaxed = true)

    @Test
    fun `HomeScreen_deve_exibir_SearchBar_quando_carregada`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.Home.Layout.Screen(homeViewModel = viewModel)
                }
            }
        }

        // Verifica se o placeholder da busca aparece
        composeTestRule.onNodeWithText("Buscar mangá...", substring = true).assertIsDisplayed()

        // Verifica se o FloatingTool (HUB) está presente
        composeTestRule.onNodeWithContentDescription("hub", substring = true).assertIsDisplayed()
    }
}
