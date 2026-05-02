package br.acerola.comic.common.ux.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class ProgressTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_porcentagem_quando_informada() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Progress(
                    isLoading = true,
                    progress = 0.45f,
                )
            }
        }

        composeTestRule.onNodeWithText("45%", substring = true).assertIsDisplayed()
    }

    @Test
    fun deve_exibir_texto_padrao_de_sincronizacao_quando_carregando_sem_progresso() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Progress(
                    isLoading = true,
                    progress = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Sincronizando", substring = true).assertIsDisplayed()
    }
}
