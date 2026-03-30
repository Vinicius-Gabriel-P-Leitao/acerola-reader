package br.acerola.manga.module.main.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.local.entity.archive.ChapterTemplate
import br.acerola.manga.module.main.Main
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import br.acerola.manga.module.main.screen.state.FilePatternUiState

class FilePatternScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_lista_de_templates_no_layout() {
        val templates = listOf(
            ChapterTemplate(id = 1, label = "Padrão Teste 1", pattern = "{value}", isDefault = true),
            ChapterTemplate(id = 2, label = "Padrão Teste 2", pattern = "Cap. {value}", isDefault = false)
        )

        val viewModel = mockk<FilePatternViewModel>(relaxed = true)
        every { viewModel.uiState } returns MutableStateFlow(FilePatternUiState(templates = templates))

        composeTestRule.setContent {
            Main.Screen.Layout.FilePatternScreen(
                onBack = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Padrão Teste 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Padrão Teste 2").assertIsDisplayed()
    }
}
