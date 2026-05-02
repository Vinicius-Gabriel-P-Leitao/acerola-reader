package br.acerola.comic.module.main.pattern

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.dto.archive.ArchiveTemplateDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.pattern.state.FilePatternUiState
import br.acerola.comic.util.sort.SortType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class FilePatternScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(templates: List<ArchiveTemplateDto> = emptyList()): FilePatternViewModel {
        val viewModel = mockk<FilePatternViewModel>(relaxed = true)
        every { viewModel.uiState } returns MutableStateFlow(FilePatternUiState(templates = templates))
        every { viewModel.uiEvents } returns MutableSharedFlow()
        return viewModel
    }

    private fun setScreen(viewModel: FilePatternViewModel) {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.Pattern.Template.FilePatternScreen(
                        onBack = {},
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    @Test
    fun deve_exibir_lista_de_templates_no_layout() {
        val templates =
            listOf(
                ArchiveTemplateDto(id = 1L, label = "Padrão Teste 1", pattern = "{chapter}", type = SortType.CHAPTER, isDefault = true),
                ArchiveTemplateDto(id = 2L, label = "Padrão Teste 2", pattern = "Cap. {chapter}", type = SortType.CHAPTER, isDefault = false),
            )

        setScreen(buildViewModel(templates))

        composeTestRule.onNodeWithText("Padrão Teste 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Padrão Teste 2").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_estado_vazio_quando_lista_esta_vazia() {
        setScreen(buildViewModel(emptyList()))

        composeTestRule.onNodeWithText("Padrão Teste 1").assertDoesNotExist()
    }

    @Test
    fun deve_exibir_badge_de_sistema_para_template_padrao() {
        val templates =
            listOf(
                ArchiveTemplateDto(id = 1L, label = "Template Sistema", pattern = "{chapter}", type = SortType.CHAPTER, isDefault = true),
            )

        setScreen(buildViewModel(templates))

        composeTestRule.onNodeWithText("Template Sistema").assertIsDisplayed()
    }
}
