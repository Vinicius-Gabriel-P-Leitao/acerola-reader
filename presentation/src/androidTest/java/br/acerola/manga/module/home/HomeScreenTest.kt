package br.acerola.manga.module.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HomeViewModel>(relaxed = true)

    @Test
    fun `HomeScreen_deve_exibir_SearchBar_e_lista_de_mangás_quando_houver_dados`() {
        val mangas = listOf(
            MangaDto(
                directory = MangaDirectoryDto(1, "Manga 1", "", null, null, 0, null),
                remoteInfo = null
            )
        )

        // Mockando os fluxos de estado do ViewModel
        every { viewModel.selectedHomeLayout } returns MutableStateFlow(HomeLayoutType.LIST)
        every { viewModel.isIndexing } returns MutableStateFlow(false)
        every { viewModel.progress } returns MutableStateFlow(-1)
        every { viewModel.mangas } returns MutableStateFlow(mangas)

        composeTestRule.setContent {
            AcerolaTheme {
                HomeScreen(homeViewModel = viewModel)
            }
        }

        // Verifica se o placeholder da busca aparece
        composeTestRule.onNodeWithText("Buscar mangá...", substring = true).assertIsDisplayed()

        // Verifica se o item da lista aparece
        composeTestRule.onNodeWithText("Manga 1").assertIsDisplayed()

        // Verifica se o FloatingTool (HUB) está presente
        composeTestRule.onNodeWithContentDescription("Abrir hub de ferramentas", substring = true).assertIsDisplayed()
    }
}
