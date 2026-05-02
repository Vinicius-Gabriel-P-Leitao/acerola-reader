package br.acerola.comic.module.reader.gesture

import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class ZoomablePageImageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_renderizar_com_bitmap_fake() {
        composeTestRule.setContent {
            Reader.Gesture.ZoomablePageImage(
                comicId = 1L,
                chapterId = 1L,
                pageIndex = 0,
                onAreaTap = {},
                onZoomStatusChange = {},
            )
        }
        composeTestRule.waitForIdle()
    }
}
