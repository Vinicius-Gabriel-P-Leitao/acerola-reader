package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Pagination
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class PaginationFooterTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Pagination_deve_exibir_o_indicador_de_paginas_corretamente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Pagination(
                    currentPage = 0,
                    totalPages = 10,
                    onPageChange = {},
                )
            }
        }

        // Verifica se o texto indicando a página atual e o total aparece conforme R.string.label_pagination_format ("%1$d / %2$d")
        composeTestRule.onNodeWithText("1 / 10").assertIsDisplayed()
    }

    @Test
    fun `Pagination_deve_chamar_onPageChange_ao_clicar_em_uma_pagina`() {
        var selectedPage = -1
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.Pagination(
                    currentPage = 0,
                    totalPages = 5,
                    onPageChange = { selectedPage = it },
                )
            }
        }

        // Clica no botão da página 3 (índice 2)
        composeTestRule.onNodeWithText("3").performClick()

        assert(selectedPage == 2)
    }
}
