package br.acerola.manga.common.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.presentation.R
import org.junit.Rule
import org.junit.Test

class ProgressIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `ProgressIndicator_deve_exibir_texto_de_sincronização_indeterminada_inicialmente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                ProgressIndicator(isLoading = true, progress = null)
            }
        }

        // Verifica se a string de carregamento padrão aparece
        val syncLabel = context.getString(R.string.label_sync_progress)
        composeTestRule.onNodeWithText(syncLabel, substring = true).assertIsDisplayed()
    }

    @Test
    fun `ProgressIndicator_deve_exibir_a_porcentagem_de_progresso_quando_disponível`() {
        composeTestRule.setContent {
            AcerolaTheme {
                ProgressIndicator(isLoading = true, progress = 0.5f)
            }
        }

        // 0.5f -> 50%
        composeTestRule.onNodeWithText("50%", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ProgressIndicator_deve_exibir_estado_concluído_com_feedback_visual_ao_finalizar`() {
        val isLoading = androidx.compose.runtime.mutableStateOf(true)

        composeTestRule.setContent {
            AcerolaTheme {
                ProgressIndicator(isLoading = isLoading.value, progress = 1f)
            }
        }

        // Muda para falso para disparar o feedback de conclusão
        isLoading.value = false

        // Aguarda a animação e o delay interno de feedback (Sincronizado!)
        val completeLabel = context.getString(R.string.label_sync_complete)
        composeTestRule.onNodeWithText(completeLabel, substring = true).assertIsDisplayed()
    }
}
