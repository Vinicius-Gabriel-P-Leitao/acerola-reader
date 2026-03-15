package br.acerola.manga.common.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.layout.ProgressIndicator
import br.acerola.manga.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class ProgressIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ProgressIndicator_deve_exibir_porcentagem_quando_o_progresso_for_informado`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Layout.ProgressIndicator(
                    isLoading = true,
                    progress = 0.45f // 45%
                )
            }
        }

        composeTestRule.onNodeWithText("45%", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ProgressIndicator_deve_mostrar_indicador_indeterminado_quando_progresso_for_nulo`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Layout.ProgressIndicator(
                    isLoading = true,
                    progress = null
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
                Acerola.Layout.ProgressIndicator(
                    isLoading = false
                )
            }
        }

        composeTestRule.onNodeWithText("Sincronizando").assertDoesNotExist()
    }
}
