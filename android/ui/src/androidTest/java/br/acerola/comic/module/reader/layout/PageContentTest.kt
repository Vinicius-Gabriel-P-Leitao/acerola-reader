package br.acerola.comic.module.reader.layout

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class PageContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve_renderizar_conteudo_do_leitor_sem_erros`() {
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 10 })
            val listState = rememberLazyListState()

            Reader.Layout.PageContent(
                pageCount = 10,
                pagerState = pagerState,
                onUiToggle = {},
                onPrevClick = {},
                onNextClick = {},
                readingMode = ReadingMode.HORIZONTAL,
                listState = listState,
                comicId = 1L,
                chapterId = 1L,
                onPageRequest = {},
                onZoomChange = {},
            )
        }

        composeTestRule.waitForIdle()
    }
}
