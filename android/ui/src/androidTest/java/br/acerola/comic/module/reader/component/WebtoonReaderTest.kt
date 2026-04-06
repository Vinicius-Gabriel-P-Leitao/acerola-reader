package br.acerola.comic.module.reader.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class WebtoonReaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve_solicitar_paginas_no_modo_webtoon`() {
        var requestedIndices = mutableSetOf<Int>()
        composeTestRule.setContent {
            val listState = rememberLazyListState()
             Reader.Component.WebtoonReader(
                pageCount = 10,
                mangaId = 1L,
                chapterId = 1L,
                listState = listState,
                onUiToggle = {},
                onPageRequest = { requestedIndices.add(it) },
                onZoomChange = {}
            )
        }

        composeTestRule.waitForIdle()
        assert(requestedIndices.contains(0))
    }
}
