package br.acerola.manga.module.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.home.HomeViewModel
import br.acerola.manga.module.main.home.Screen
import br.acerola.manga.presentation.R
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class HomeLayoutToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HomeViewModel>(relaxed = true)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `clique_no_botão_de_layout_deve_alternar_entre_lista_e_grade`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.Home.Layout.Screen(homeViewModel = viewModel)
                }
            }
        }

        // 1. Abre o HUB de ferramentas
        val hubDescription = context.getString(R.string.description_icon_home_floating_tool_hub)
        composeTestRule.onNodeWithContentDescription(hubDescription, substring = true).performClick()

        // Aguarda animação de expansão do FloatingTool
        composeTestRule.waitForIdle()
        
        // Verifica se o botão de mudar layout está visível no HUB
        val layoutToggleDescription = context.getString(R.string.description_icon_home_change_layout)
        composeTestRule.onNodeWithContentDescription(layoutToggleDescription, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
    }
}
