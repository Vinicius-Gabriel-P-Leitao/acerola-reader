package br.acerola.comic.module.main.pattern

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.dto.archive.ChapterTemplateDto
import br.acerola.comic.module.main.Main
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import br.acerola.comic.module.main.pattern.state.FilePatternUiState

class FilePatternScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_lista_de_templates_no_layout() {
                val templates = listOf(
            ChapterTemplateDto(id = 1L, label = "Padrão Teste 1", pattern = "{chapter}", isDefault = true),
            ChapterTemplateDto(id = 2L, label = "Padrão Teste 2", pattern = "Cap. {chapter}", isDefault = false)
        )

        val viewModel = mockk<FilePatternViewModel>(relaxed = true)
        every { viewModel.uiState } returns MutableStateFlow(FilePatternUiState(templates = templates))

        composeTestRule.setContent {
            Main.Pattern.Layout.FilePatternScreen(
                onBack = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Padrão Teste 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Padrão Teste 2").assertIsDisplayed()
    }
}
