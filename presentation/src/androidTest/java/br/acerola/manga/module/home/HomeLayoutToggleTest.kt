package br.acerola.manga.module.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.config.preference.HomeLayoutType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class HomeLayoutToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HomeViewModel>(relaxed = true)

    @Test
    fun `clique_no_botão_de_layout_deve_alternar_entre_lista_e_grade`() {
        every { viewModel.selectedHomeLayout } returns MutableStateFlow(HomeLayoutType.LIST)
        every { viewModel.isIndexing } returns MutableStateFlow(false)
        every { viewModel.progress } returns MutableStateFlow(-1)
        every { viewModel.mangas } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            AcerolaTheme {
                HomeScreen(homeViewModel = viewModel)
            }
        }

        // 1. Abre o HUB de ferramentas
        composeTestRule.onNodeWithContentDescription("Abrir hub de ferramentas", substring = true).performClick()

        // 2. Clica no botão de trocar visualização (que deve exibir "Grade" já que estamos em Lista)
        composeTestRule.onNodeWithText("Grade", substring = true).performClick()

        // 3. Verifica se o ViewModel foi notificado para mudar o layout
        verify { viewModel.updateHomeLayout(HomeLayoutType.GRID) }
    }
}
