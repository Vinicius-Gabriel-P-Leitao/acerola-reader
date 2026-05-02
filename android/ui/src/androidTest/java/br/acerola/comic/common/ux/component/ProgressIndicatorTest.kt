package br.acerola.comic.common.ux.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class ProgressIndicatorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ProgressIndicator_deve_exibir_porcentagem_quando_o_progresso_for_informado`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Progress(
                    isLoading = true,
                    progress = 0.45f, // 45%
                )
            }
        }

        composeTestRule.onNodeWithText("45%", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ProgressIndicator_deve_mostrar_indicador_indeterminado_quando_progresso_for_nulo`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Progress(
                    isLoading = true,
                    progress = null,
                )
            }
        }

        // Como o progresso é nulo, verificamos se o texto padrão de sincronização aparece
        composeTestRule.onNodeWithText("Sincronizando", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ProgressIndicator_deve_desaparecer_quando_não_estiver_carregando`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Progress(
                    isLoading = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Sincronizando").assertDoesNotExist()
    }
}
