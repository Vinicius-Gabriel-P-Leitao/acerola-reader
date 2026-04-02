package br.acerola.manga.module.reader

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.module.reader.state.ReaderUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<ReaderViewModel>(relaxed = true)

    @Before
    fun setUp() {
        every { viewModel.state } returns MutableStateFlow(ReaderUiState(isLoading = true))
        every { viewModel.uiEvents } returns MutableSharedFlow<UserMessage>().asSharedFlow()
    }

    @Test
    fun `ReaderScreen_deve_renderizar_sem_erros`() {
        val chapter = ChapterFileDto(1L, "Cap 1", "path", "1")
        
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    ReaderScreen(
                        chapter = chapter,
                        initialPage = 0,
                        mangaId = 1L,
                        onBackClick = {},
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }
}
