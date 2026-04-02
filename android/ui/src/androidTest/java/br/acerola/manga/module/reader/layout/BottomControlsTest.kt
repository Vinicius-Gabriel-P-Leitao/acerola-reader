package br.acerola.manga.module.reader.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class BottomControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderBottomControls_deve_exibir_o_número_da_página_atual_corretamente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Layout.BottomControls(
                    pageCount = 50,
                    currentPage = 9, // Página 10
                    onPrevClick = {},
                    onNextClick = {},
                    onNextChapterClick = {},
                    onPreviousChapterClick = {}
                )
            }
        }

        // Verifica se a string \"10 / 50\" está presente
        composeTestRule.onNodeWithText("10 / 50").assertIsDisplayed()
    }

    @Test
    fun `botão_de_próximo_capítulo_deve_aparecer_apenas_quando_capítulo_estiver_lido_e_houver_próximo`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Layout.BottomControls(
                    pageCount = 10,
                    currentPage = 9,
                    isChapterRead = true,
                    hasNextChapter = true,
                    onPrevClick = {},
                    onNextClick = {},
                    onNextChapterClick = {},
                    onPreviousChapterClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Próximo", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `botão_de_capítulo_anterior_deve_aparecer_quando_houver_anterior`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Layout.BottomControls(
                    pageCount = 10,
                    currentPage = 0,
                    hasPreviousChapter = true,
                    onPrevClick = {},
                    onNextClick = {},
                    onNextChapterClick = {},
                    onPreviousChapterClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Anterior", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
