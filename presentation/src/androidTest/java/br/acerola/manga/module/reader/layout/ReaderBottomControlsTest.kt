package br.acerola.manga.module.reader.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class ReaderBottomControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderBottomControls_deve_exibir_a_contagem_de_páginas_no_formato_correto`() {
        composeTestRule.setContent {
            AcerolaTheme {
                ReaderBottomControls(
                    pageCount = 20,
                    currentPage = 5,
                    onPrevClick = {},
                    onNextClick = {}
                )
            }
        }

        // Index 5 corresponde à página visual 6
        composeTestRule.onNodeWithText("Página 6 / 20").assertIsDisplayed()
    }

    @Test
    fun `botão_de_página_anterior_deve_estar_desabilitado_na_primeira_página`() {
        composeTestRule.setContent {
            AcerolaTheme {
                ReaderBottomControls(
                    pageCount = 10,
                    currentPage = 0,
                    onPrevClick = {},
                    onNextClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Página Anterior").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Próximo").assertIsEnabled()
    }

    @Test
    fun `clique_no_botão_Próximo_deve_disparar_a_ação_de_navegação`() {
        var nextCalled = false
        composeTestRule.setContent {
            AcerolaTheme {
                ReaderBottomControls(
                    pageCount = 10,
                    currentPage = 5,
                    onPrevClick = {},
                    onNextClick = { nextCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Próximo").performClick()
        assert(nextCalled)
    }
}
