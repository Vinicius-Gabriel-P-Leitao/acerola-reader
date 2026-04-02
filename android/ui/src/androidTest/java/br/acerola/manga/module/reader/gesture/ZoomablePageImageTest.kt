package br.acerola.manga.module.reader.gesture

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class ZoomablePageImageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_renderizar_com_bitmap_fake() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        composeTestRule.setContent {
            Reader.Gesture.ZoomablePageImage(
                pageBitmap = bitmap,
                onAreaTap = {},
                onZoomStatusChange = {}
            )
        }
        composeTestRule.waitForIdle()
    }
}
