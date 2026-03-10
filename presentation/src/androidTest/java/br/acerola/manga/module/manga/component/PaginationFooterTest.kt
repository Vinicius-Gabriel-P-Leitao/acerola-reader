package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class PaginationFooterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `PaginationFooter_deve_organizar_os_blocos_de_páginas_conforme_o_índice_atual`() {
        var pageTarget = -1

        composeTestRule.setContent {
            AcerolaTheme {
                PaginationFooter(
                    currentPage = 0,
                    totalPages = 10,
                    onPageChange = { pageTarget = it }
                )
            }
        }

        // Valida contagem textual (1 / 10)
        composeTestRule.onNodeWithText("1 / 10", substring = true).assertIsDisplayed()

        // Valida botões do bloco (1 a 5)
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("6").assertDoesNotExist()

        // Aciona clique em página específica
        composeTestRule.onNodeWithText("3").performClick()
        assert(pageTarget == 2)
    }

    @Test
    fun `botão_de_navegação_deve_alternar_para_a_página_seguinte_corretamente`() {
        var pageTarget = -1

        composeTestRule.setContent {
            AcerolaTheme {
                PaginationFooter(
                    currentPage = 0,
                    totalPages = 10,
                    onPageChange = { pageTarget = it }
                )
            }
        }

        // Aciona ícone de próxima página
        composeTestRule.onNodeWithContentDescription("Próxima Página").performClick()
        assert(pageTarget == 1)
    }
}
