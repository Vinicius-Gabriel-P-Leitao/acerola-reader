package br.acerola.comic.module.main.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.error.UserMessage
import br.acerola.comic.module.main.Main
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HistoryViewModel>(relaxed = true)

    @Before
    fun setUp() {
        every { viewModel.historyItems } returns MutableStateFlow(emptyList())
        every { viewModel.uiEvents } returns MutableSharedFlow<UserMessage>().asSharedFlow()
    }

    @Test
    fun `HistoryScreen_deve_exibir_estado_vazio_quando_sem_historico`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.History.Template.Screen(viewModel = viewModel)
                }
            }
        }

        composeTestRule.onNodeWithText("Nenhum quadrinho lido recentemente").assertIsDisplayed()
    }
}
