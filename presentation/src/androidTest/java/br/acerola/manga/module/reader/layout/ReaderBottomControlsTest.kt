package br.acerola.manga.module.reader.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.presentation.R
import org.junit.Rule
import org.junit.Test

class ReaderBottomControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `ReaderBottomControls_deve_exibir_a_contagem_de_páginas_no_formato_correto`() {
        composeTestRule.setContent {
            _root_ide_package_.br.acerola.manga.common.ux.theme.AcerolaTheme {
                BottomControls(
                    pageCount = 20,
                    currentPage = 5,
                    onPrevClick = {},
                    onNextClick = {}
                )
            }
        }

        // Index 5 corresponde à página visual 6
        val pageFormat = context.getString(R.string.label_reader_page_format, 6, 20)
        composeTestRule.onNodeWithText(pageFormat).assertIsDisplayed()
    }

    @Test
    fun `botão_de_página_anterior_deve_estar_desabilitado_na_primeira_página`() {
        composeTestRule.setContent {
            _root_ide_package_.br.acerola.manga.common.ux.theme.AcerolaTheme {
                BottomControls(
                    pageCount = 10,
                    currentPage = 0,
                    onPrevClick = {},
                    onNextClick = {}
                )
            }
        }

        val prevDescription = context.getString(R.string.description_icon_pagination_previous)
        val nextLabel = context.getString(R.string.label_reader_next_page)
        
        composeTestRule.onNodeWithContentDescription(prevDescription).assertIsNotEnabled()
        composeTestRule.onNodeWithText(nextLabel).assertIsEnabled()
    }

    @Test
    fun `clique_no_botão_Próximo_deve_disparar_a_ação_de_navegação`() {
        var nextCalled = false
        composeTestRule.setContent {
            _root_ide_package_.br.acerola.manga.common.ux.theme.AcerolaTheme {
                BottomControls(
                    pageCount = 10,
                    currentPage = 5,
                    onPrevClick = {},
                    onNextClick = { nextCalled = true }
                )
            }
        }

        val nextLabel = context.getString(R.string.label_reader_next_page)
        composeTestRule.onNodeWithText(nextLabel).performClick()
        assert(nextCalled)
    }
}
