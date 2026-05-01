package br.acerola.comic.module.reader.component

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class HorizontalPagedReaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve_solicitar_pagina_ao_exibir_index_do_pager`() {
        var requestedIndex = -1
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 10 })
            Reader.Component.HorizontalPagedReader(
                pageCount = 10,
                comicId = 1L,
                chapterId = 1L,
                pagerState = pagerState,
                onUiToggle = {},
                onPrevClick = {},
                onNextClick = {},
                onPageRequest = { requestedIndex = it },
                onZoomChange = {},
            )
        }

        // Como o initialPage é 0, deve solicitar a página 0
        composeTestRule.waitUntil(timeoutMillis = 2000) { requestedIndex == 0 }
        assert(requestedIndex == 0)
    }
}
