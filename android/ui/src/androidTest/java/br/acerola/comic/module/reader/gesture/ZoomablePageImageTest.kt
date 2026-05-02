package br.acerola.comic.module.reader.gesture

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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

    @Test
    fun deve_identificar_clique_no_centro_da_imagem() {
        var tappedArea: br.acerola.comic.module.reader.state.TapArea? = null
        composeTestRule.setContent {
            Reader.Gesture.ZoomablePageImage(
                comicId = 1L,
                chapterId = 1L,
                pageIndex = 0,
                onAreaTap = { tappedArea = it },
                onZoomStatusChange = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("zoomable_image").performTouchInput {
            click(center)
        }

        // Aguarda reconhecimento do gesto
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            tappedArea == br.acerola.comic.module.reader.state.TapArea.CENTER
        }

        assert(tappedArea == br.acerola.comic.module.reader.state.TapArea.CENTER)
    }
}
