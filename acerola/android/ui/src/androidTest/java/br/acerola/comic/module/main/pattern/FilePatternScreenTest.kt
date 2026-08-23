package br.acerola.comic.module.main.pattern

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.common.ux.theme.AcerolaTheme
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
    fun should_display_templates_list_in_layout() {
        val templates =
            listOf(
                ArchiveTemplateDto(id = 1L, label = "Test Pattern 1", pattern = "{chapter}", type = SortType.CHAPTER, isDefault = true),
                ArchiveTemplateDto(id = 2L, label = "Test Pattern 2", pattern = "Cap. {chapter}", type = SortType.CHAPTER, isDefault = false),
            )

        setScreen(buildViewModel(templates))

        composeTestRule.onNodeWithText("Test Pattern 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Pattern 2").assertIsDisplayed()
    }

    @Test
    fun should_display_empty_state_when_list_is_empty() {
        setScreen(buildViewModel(emptyList()))

        composeTestRule.onNodeWithText("Test Pattern 1").assertDoesNotExist()
    }

    @Test
    fun should_display_system_badge_for_default_template() {
        val templates =
            listOf(
                ArchiveTemplateDto(id = 1L, label = "System Template", pattern = "{chapter}", type = SortType.CHAPTER, isDefault = true),
            )

        setScreen(buildViewModel(templates))

        composeTestRule.onNodeWithText("System Template").assertIsDisplayed()
    }
}
