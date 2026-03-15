package br.acerola.manga.module.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.presentation.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MainLayoutToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HomeViewModel>(relaxed = true)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `clique_no_botão_de_layout_deve_alternar_entre_lista_e_grade`() {
        every { viewModel.selectedHomeLayout } returns MutableStateFlow(HomeLayoutType.LIST)
        every { viewModel.isIndexing } returns MutableStateFlow(false)
        every { viewModel.progress } returns MutableStateFlow(-1)
        every { viewModel.mangas } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            AcerolaTheme {
                Home.Layout.Screen(homeViewModel = viewModel)
            }
        }

        // 1. Abre o HUB de ferramentas
        val hubDescription = context.getString(R.string.description_icon_home_floating_tool_hub)
        composeTestRule.onNodeWithContentDescription(hubDescription, substring = true).performClick()

        // Aguarda animação de expansão do FloatingTool
        composeTestRule.waitForIdle()
        val gridLabel = context.getString(R.string.description_text_home_layout_grid_label)
        
        // Usamos useUnmergedTree para garantir que encontramos o texto dentro do FloatingTool
        composeTestRule.onNodeWithText(gridLabel, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        // 3. Verifica se o ViewModel foi notificado para mudar o layout
        verify { viewModel.updateHomeLayout(HomeLayoutType.GRID) }
    }
}
