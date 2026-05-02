package br.acerola.comic.module.reader.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class WebtoonReaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_solicitar_paginas_no_modo_webtoon() {
        var requestedIndices = mutableSetOf<Int>()
        composeTestRule.setContent {
            val listState = rememberLazyListState()
            Reader.Component.WebtoonReader(
                pageCount = 10,
                comicId = 1L,
                chapterId = 1L,
                listState = listState,
                onUiToggle = {},
                onPageRequest = { requestedIndices.add(it) },
                onZoomChange = {},
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) { requestedIndices.contains(0) }
        assert(requestedIndices.contains(0))
    }

    @Test
    fun deve_alternar_UI_ao_clicar_na_imagem() {
        var uiToggled = false
        composeTestRule.setContent {
            Reader.Component.WebtoonReader(
                pageCount = 1,
                comicId = 1L,
                chapterId = 1L,
                listState = rememberLazyListState(),
                onUiToggle = { uiToggled = true },
                onPageRequest = {},
                onZoomChange = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("webtoon_page_0").performTouchInput {
            click(center)
        }

        // Aguarda a atualização do estado
        composeTestRule.waitUntil(timeoutMillis = 3000) { uiToggled }
        assert(uiToggled)
    }
}
