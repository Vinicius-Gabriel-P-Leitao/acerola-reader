package br.acerola.manga.module.reader.layout

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class ReaderContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderContent_deve_renderizar_sem_erros_nos_modos_paginados`() {
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 10 })
            val listState = rememberLazyListState()

            AcerolaTheme {
                Reader.Layout.PageContent(
                    pageCount = 10,
                    pagerState = pagerState,
                    onUiToggle = {},
                    onPrevClick = {},
                    onNextClick = {},
                    readingMode = ReadingMode.HORIZONTAL,
                    listState = listState,
                    pages = emptyMap(),
                    onPageRequest = {},
                    onZoomChange = {}
                )
            }
        }

        // Smoke test: se chegou aqui sem crashar, o layout básico foi montado
        composeTestRule.waitForIdle()
    }
}
