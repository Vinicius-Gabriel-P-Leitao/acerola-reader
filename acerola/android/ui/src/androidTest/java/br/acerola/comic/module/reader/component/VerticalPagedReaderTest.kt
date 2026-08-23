package br.acerola.comic.module.reader.component

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class VerticalPagedReaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should_render_vertical_reader_without_errors`() {
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 10 })
            Reader.Component.VerticalPagedReader(
                comicId = 1L,
                chapterId = 1L,
                pagerState = pagerState,
                onUiToggle = {},
                onPrevClick = {},
                onNextClick = {},
                onZoomChange = {},
            )
        }
    }
}
