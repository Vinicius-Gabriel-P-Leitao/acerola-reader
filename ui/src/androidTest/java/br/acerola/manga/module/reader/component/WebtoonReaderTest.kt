package br.acerola.manga.module.reader.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.module.reader.Reader
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
                pages = emptyMap(),
                listState = listState,
                pageCount = 10,
                onUiToggle = {},
                onPageRequest = { requestedIndices.add(it) },
                onZoomChange = {}
            )
        }

        composeTestRule.waitForIdle()
        assert(requestedIndices.contains(0))
    }
}
