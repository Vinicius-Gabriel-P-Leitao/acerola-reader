package br.acerola.manga.module.reader.component

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class VerticalPagedReaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve_solicitar_pagina_ao_exibir_index_do_pager_vertical`() {
        var requestedIndex = -1
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 10 })
            Reader.Component.VerticalPagedReader(
                pages = emptyMap(),
                pagerState = pagerState,
                onUiToggle = {},
                onPrevClick = {},
                onNextClick = {},
                onPageRequest = { requestedIndex = it },
                onZoomChange = {}
            )
        }

        composeTestRule.waitForIdle()
        assert(requestedIndex == 0)
    }
}
