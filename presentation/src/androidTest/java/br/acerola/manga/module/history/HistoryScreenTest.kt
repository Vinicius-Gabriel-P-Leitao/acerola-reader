package br.acerola.manga.module.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.history.HistoryViewModel
import br.acerola.manga.module.main.history.Screen
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<HistoryViewModel>(relaxed = true)

    @Test
    fun `HistoryScreen_deve_exibir_o_titulo_da_tela`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.History.Layout.Screen(viewModel = viewModel)
                }
            }
        }

        composeTestRule.onNodeWithText("Histórico", substring = true).assertIsDisplayed()
    }
}
