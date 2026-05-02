package br.acerola.comic.common.activity

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Progress
import br.acerola.comic.common.ux.component.SnackbarError
import br.acerola.comic.common.ux.component.SnackbarSuccess
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.showSnackbar
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class BaseActivityIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_snackbar_de_erro_quando_solicitado_via_LocalSnackbarHostState() {
        val snackbarHostState = SnackbarHostState()

        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                    androidx.compose.material3.Scaffold(
                        snackbarHost = {
                            androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) { data ->
                                Acerola.Component.SnackbarError(message = data.visuals.message)
                            }
                        },
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar("Erro de teste", SnackbarVariant.Error)
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Erro de teste").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_snackbar_de_sucesso_quando_solicitado() {
        val snackbarHostState = SnackbarHostState()

        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                    androidx.compose.material3.Scaffold(
                        snackbarHost = {
                            androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) { data ->
                                Acerola.Component.SnackbarSuccess(message = data.visuals.message)
                            }
                        },
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar("Sucesso!", SnackbarVariant.Success)
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Sucesso!").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_indicador_de_progresso_global_quando_o_app_estiver_sincronizando() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Progress(
                    isLoading = true,
                    progress = 0.5f,
                )
            }
        }

        composeTestRule.onNodeWithText("50%", substring = true).assertIsDisplayed()
    }
}
