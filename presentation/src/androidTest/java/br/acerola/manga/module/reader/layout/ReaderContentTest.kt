package br.acerola.manga.module.reader.layout

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.config.preference.ReadingMode
import org.junit.Rule
import org.junit.Test

class ReaderContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderContent_deve_renderizar_corretamente_sem_lançar_exceções`() {
        composeTestRule.setContent {
            // Inicializa os estados dentro do contexto do Compose
            val pagerState = rememberPagerState { 5 }
            val listState = rememberLazyListState()
            
            AcerolaTheme {
                ReaderContent(
                    pageCount = 5,
                    pagerState = pagerState,
                    listState = listState,
                    readingMode = ReadingMode.HORIZONTAL,
                    pages = emptyMap(),
                    onUiToggle = {},
                    onPrevClick = {},
                    onNextClick = {},
                    onPageRequest = {},
                    onZoomChange = {}
                )
            }
        }

        // Valida se a raiz da interface foi construída com sucesso
        composeTestRule.onRoot().assertExists()
    }
}
